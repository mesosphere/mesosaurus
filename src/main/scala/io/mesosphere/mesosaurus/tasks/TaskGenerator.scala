package io.mesosphere.mesosaurus.tasks

import io.mesosphere.mesosaurus._
import org.apache.mesos.Protos._
import scala.collection.JavaConverters._
import scala.collection.mutable
import java.net.URI

/**
  * Generates descriptions of tasks with a variety of configurable properties
  * like average arrival time, duration, resource consumption.
  */
class TaskGenerator(
    artifacts: Seq[URI],
    requestedTasks: Int,
    taskDurationMean: Int,
    taskDurationSigma: Int,
    arrivalTimeMean: Int,
    load: Double,
    cpusMean: Double,
    cpusSigma: Double,
    memMean: Long,
    memSigma: Long,
    offerAttempts: Int = 100) extends Logging {

  private var _createdTasks = 0
  private var _forfeitedTasks = 0

  /**
    * Whether any more tasks will be created.
    * This is supposed to be queried by the scheduler to determine when to decline all offers.
    */
  def doneCreatingTasks(): Boolean = {
    return _createdTasks + _forfeitedTasks >= requestedTasks
  }

  private var _terminatedTasks = 0

  /**
    * Whether all existing tasks are done executing or to be finished immediately
    * and no more tasks are to be scheduled.
    * This is supposed to be queried by the scheduler to determine
    * when to stop the scheduler and thus terminate the whole framework.
    */
  def done(): Boolean = {
    return _terminatedTasks >= requestedTasks && doneCreatingTasks()
  }

  def observeTaskStatusUpdate(taskStatus: TaskStatus) = {
    taskStatus.getState() match {
      case TaskState.TASK_FAILED |
        TaskState.TASK_KILLED |
        TaskState.TASK_LOST => {
        log.info("task failed: " + taskStatus)
        _terminatedTasks += 1;
      }
      case TaskState.TASK_FINISHED => {
        _terminatedTasks += 1;
        TaskTracker.finished(taskStatus.getTaskId.getValue, Timestamp.now)
      }
      case TaskState.TASK_STAGING |
        TaskState.TASK_STARTING |
        TaskState.TASK_RUNNING =>
        TaskTracker.started(taskStatus.getTaskId.getValue, Timestamp.now)
    }
  }

  val TASK_PROGRAM = "mesosaurus-task"

  private def createTaskInfo(slaveID: SlaveID, taskDescriptor: TaskDescriptor): TaskInfo = {
    val taskID = TaskID.newBuilder()
      .setValue(Integer.toString(taskDescriptor.id))
      .build()
    System.out.println("Launching task " + taskID.getValue())

    val uris: Seq[CommandInfo.URI] = artifacts.map { artifact =>
      CommandInfo.URI.newBuilder
        .setValue(artifact.toString)
        .setExecutable(true)
        .build
    }

    val commandInfo = CommandInfo.newBuilder()
      // .setValue("./" + TASK_PROGRAM + " " + taskDescriptor.commandArguments())
      .setValue("sleep " + taskDescriptor.duration / 1000)
      .addAllUris(uris.asJava)

    return TaskInfo.newBuilder()
      .setName("task " + taskID.getValue())
      .setTaskId(taskID)
      .setSlaveId(slaveID)
      .addResources(Resource.newBuilder()
        .setName("cpus")
        .setType(Value.Type.SCALAR)
        .setScalar(Value.Scalar.newBuilder().setValue(taskDescriptor.resources.cpus)))
      .addResources(Resource.newBuilder()
        .setName("mem")
        .setType(Value.Type.SCALAR)
        .setScalar(Value.Scalar.newBuilder().setValue(taskDescriptor.resources.mem)))
      .setCommand(commandInfo)
      .build()
  }

  private case class TaskDescriptor(
      id: Int,
      arrivalTime: Int,
      duration: Int,
      resources: Resources) extends Logging {

    var offerAttempts = 0

    def commandArguments(): String = {
      val cores = math.ceil(resources.cpus).toInt
      s"$duration $cores $load ${resources.mem}"
    }

    def print(): Unit = log.info(
      "arrival: " + arrivalTime +
        ", duration: " + duration +
        ", cpus : " + resources.cpus +
        ", mem: " + resources.mem
    )

  }

  lazy val startTime = System.currentTimeMillis

  private var _taskDescriptors: mutable.Queue[TaskDescriptor] = {
    val arrivalTimeRandom = new PoissonRandom(arrivalTimeMean)
    val durationRandom = new GaussRandom(taskDurationMean, taskDurationSigma)
    val cpusRandom = new GaussRandom(cpusMean, cpusSigma)
    val memRandom = new GaussRandom(memMean.toDouble, memSigma.toDouble)

    val queue = mutable.Queue[TaskDescriptor]()

    var arrivalTime = 0

    for (id <- 0 until requestedTasks) {
      arrivalTime += arrivalTimeRandom.next().toInt
      val duration = durationRandom.next().toInt
      val cpus = cpusRandom.next()
      val mem = memRandom.next().toLong
      val resources = new Resources(cpus, mem)
      val td = TaskDescriptor(id, arrivalTime, duration, resources)
      queue += td
      log.info(s"Created [$td]")
      TaskTracker.arrived(s"$id", Timestamp(startTime + arrivalTime))
    }

    queue
  }

  def numPendingTasks(): Int = {
    val elapsedTime = Timestamp.now.millis - startTime
    _taskDescriptors.count { _.arrivalTime <= elapsedTime }
  }

  /**
    * NB: This method removes tasks that have exceeded the maximum number of
    * offer attempts from the queue of task queue as a side effect!
    */
  def generateTaskInfos(offer: Offer): java.util.Collection[TaskInfo] = {
    var offerResources = new Resources(offer)
    val elapsedTime = Timestamp.now.millis - startTime

    // Remove tasks that have exceeded the number of offer attempts
    val (forfeited, retained) =
      _taskDescriptors.partition(_.offerAttempts > offerAttempts)

    log.info(s"Forfeiting [${forfeited.size} tasks]")
    _taskDescriptors = mutable.Queue(retained: _*)
    _forfeitedTasks += forfeited.size

    val taskInfos = mutable.Buffer[TaskInfo]()

    log.info("Queued tasks: [%s]" format _taskDescriptors.map(_.id).mkString(", "))

    val (scheduled, notScheduled) =
      _taskDescriptors.partition { td =>
        val arrived = td.arrivalTime <= elapsedTime
        val matchesOffer = td.resources <= offerResources
        (arrived, matchesOffer) match {
          case (true, false) =>
            td.offerAttempts += 1
            false
          case (true, true) =>
            td.offerAttempts += 1
            taskInfos += createTaskInfo(offer.getSlaveId, td)
            offerResources = offerResources - td.resources
            _createdTasks += 1
            true
          case _ => false
        }
      }

    log.info("Scheduled tasks: [%s]" format scheduled.map(_.id).mkString(", "))
    log.info("Unscheduled tasks: [%s]" format notScheduled.map(_.id).mkString(", "))

    _taskDescriptors = mutable.Queue(notScheduled: _*)
    taskInfos.asJava
  }
}
