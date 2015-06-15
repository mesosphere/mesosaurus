package io.mesosphere.mesosaurus

import org.apache.mesos.Protos._

/**
  * Generates descriptions of tasks with a variety of configurable properties
  * like average arrival time, duration, resource consumption.
  */
class TaskGenerator(requestedTasks: Int,
        taskDurationMean: Int,
        taskDurationSigma: Int,
        arrivalTimeMean: Int,
        load: Double,
        cpusMean: Double,
        cpusSigma: Double,
        memMean: Long,
        memSigma: Long,
        offerAttempts: Int = 100,
        failRate: Double = 0.0) extends Logging {

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
                println("Terminated Task = " + _terminatedTasks.toString())
            }
            case TaskState.TASK_FINISHED => {
                _terminatedTasks += 1;
            }
            case TaskState.TASK_STAGING |
                TaskState.TASK_STARTING |
                TaskState.TASK_RUNNING =>
        }
    }

    val TASK_PROGRAM = "mesosaurus-task"

    private def createTaskInfo(slaveID: SlaveID, taskDescriptor: TaskDescriptor): TaskInfo = {
        val taskID = TaskID.newBuilder()
            .setValue(Integer.toString(_createdTasks))
            .build()
        System.out.println("Launching task " + taskID.getValue())
        val uri = CommandInfo.URI.newBuilder().setValue(WebServer.url() + "/" + TASK_PROGRAM).setExecutable(true)
        val commandInfo = CommandInfo.newBuilder()
            .setValue("./" + TASK_PROGRAM + " " + taskDescriptor.commandArguments())
            .addUris(uri)
        return TaskInfo.newBuilder()
            .setName("task" + taskID.getValue())
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

    private class TaskDescriptor(val arrivalTime: Int, val duration: Int, val resources: Resources, val failRate: Double) extends Logging {
        var offerAttempts = 0

        def commandArguments(): String = {
            val cores = math.ceil(resources.cpus).toInt
            return duration + " " + cores + " " + load + " " + resources.mem + " " + failRate
        }

        def print(): Unit = {
            log.info("arrival: " + arrivalTime +
                ", duration: " + duration +
                ", cpus : " + resources.cpus +
                ", mem: " + resources.mem +
                ", failRate" + failRate)
        }
    }

    private val _taskDescriptors = new Ring[TaskDescriptor](null)

    private def prepareTasks() = {
        val arrivalTimeRandom = new PoissonRandom(arrivalTimeMean)
        val durationRandom = new GaussRandom(taskDurationMean, taskDurationSigma)
        val cpusRandom = new GaussRandom(cpusMean, cpusSigma)
        val memRandom = new GaussRandom(memMean.toDouble, memSigma.toDouble)

        var arrivalTime = 0
        for (i <- 0 until requestedTasks) {
            arrivalTime += arrivalTimeRandom.next().toInt
            val duration = durationRandom.next().toInt
            val cpus = cpusRandom.next()
            val mem = memRandom.next().toLong
            val resources = new Resources(cpus, mem)
            val taskDescriptor = new TaskDescriptor(arrivalTime, duration, resources, failRate)
            _taskDescriptors.add(taskDescriptor)
        }
    }

    prepareTasks()

    private var _startTime = System.currentTimeMillis

    def start() = {
        _startTime = System.currentTimeMillis
    }

    def generateTaskInfos(offer: Offer): java.util.Collection[TaskInfo] = {
        var offerResources = new Resources(offer)
        val taskInfos = new java.util.ArrayList[TaskInfo]()
        val currentRunTime = System.currentTimeMillis - _startTime
        var t = _taskDescriptors.next
        while (t != _taskDescriptors && t.value.arrivalTime <= currentRunTime) {
            if (t.value.resources <= offerResources) {
                taskInfos.add(createTaskInfo(offer.getSlaveId(), t.value))
                offerResources = offerResources - t.value.resources
                _createdTasks += 1
                t = t.remove()

            }
            else {
                if (t.value.offerAttempts >= offerAttempts) {
                    _forfeitedTasks += 1
                    t = t.remove()
                }
                else {
                    t.value.offerAttempts += 1
                    t = t.next
                }
            }
        }
        return taskInfos
    }
}
