package io.mesosphere.mesosaurus

import io.mesosphere.mesosaurus.tasks._
import org.apache.mesos.{ Scheduler, SchedulerDriver }
import org.apache.mesos.Protos._
import scala.collection.JavaConverters._

/**
  * Mesos scheduler for the Mesosaurus framework.
  *
  * Delegates to a task source to generate (task info) descriptions of tasks to run.
  */
class MesosaurusScheduler(private val _taskGenerator: TaskGenerator)
    extends Scheduler with Logging {

  def registered(
    driver: SchedulerDriver,
    frameworkId: FrameworkID,
    masterInfo: MasterInfo): Unit = {
    log.info("Scheduler.registered")
  }

  def reregistered(
    driver: SchedulerDriver,
    masterInfo: MasterInfo): Unit = {
    log.info("Scheduler.reregistered")
  }

  private val _filters = Filters.newBuilder().setRefuseSeconds(1).build()

  def resourceOffers(driver: SchedulerDriver, offers: java.util.List[Offer]): Unit = {

    TaskTracker.queueLength(_taskGenerator.numPendingTasks, Timestamp.now)

    if (_taskGenerator.doneCreatingTasks()) {
      for (offer <- offers.asScala) {
        driver.declineOffer(offer.getId());
      }
    }
    else {
      log.info("Scheduler.resourceOffers")
      for (offer <- offers.asScala) {
        TaskTracker.queueLength(_taskGenerator.numPendingTasks, Timestamp.now)
        val taskInfos = _taskGenerator.generateTaskInfos(offer)
        for (taskInfo <- taskInfos.asScala) {
          TaskTracker.launched(taskInfo.getTaskId.getValue, Timestamp.now)
        }
        driver.launchTasks(Seq(offer.getId).asJava, taskInfos, _filters);
      }
    }
  }

  def offerRescinded(
    driver: SchedulerDriver,
    offerId: OfferID): Unit = {
    log.info("Scheduler.offerRescinded")
  }

  def statusUpdate(driver: SchedulerDriver, taskStatus: TaskStatus): Unit = {
    log.info("Scheduler.statusUpdate")
    TaskTracker.queueLength(_taskGenerator.numPendingTasks, Timestamp.now)
    _taskGenerator.observeTaskStatusUpdate(taskStatus);
    if (_taskGenerator.done()) {
      driver.stop();
    }
  }

  def frameworkMessage(
    driver: SchedulerDriver,
    executorId: ExecutorID,
    slaveId: SlaveID,
    data: Array[Byte]): Unit = {
    log.info("Scheduler.frameworkMessage")
  }

  def disconnected(driver: SchedulerDriver): Unit = {
    ???
  }

  def slaveLost(
    driver: SchedulerDriver,
    slaveId: SlaveID): Unit = {
    log.info("Scheduler.slaveLost")
  }

  def executorLost(
    driver: SchedulerDriver,
    executorId: ExecutorID,
    slaveId: SlaveID,
    status: Int): Unit = {
    log.info("Scheduler.executorLost")
  }

  def error(driver: SchedulerDriver, message: String): Unit = {
    log.info("Scheduler.error")
  }

}
