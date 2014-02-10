package io.mesosphere.mesosaurus

import org.apache.mesos.Protos._

/**
  * Generates infos for a fixed number of small tasks, one for each offer received.
  */
class SimpleTaskSource(private val _nTasksRequested: Int) extends TaskSource {

  private var _nTasksCreated = 0;

  def doneCreatingTasks(): Boolean = {
    return _nTasksCreated >= _nTasksRequested
  }

  private def createTaskInfo(slaveID: SlaveID): TaskInfo = {
    val taskID = TaskID.newBuilder()
      .setValue(Integer.toString(_nTasksCreated))
      .build();
    System.out.println("Launching task " + taskID.getValue());
    return TaskInfo.newBuilder()
      .setName("task " + taskID.getValue())
      .setTaskId(taskID)
      .setSlaveId(slaveID)
      .addResources(Resource.newBuilder()
        .setName("cpus")
        .setType(Value.Type.SCALAR)
        .setScalar(Value.Scalar.newBuilder().setValue(1)))
      .addResources(Resource.newBuilder()
        .setName("mem")
        .setType(Value.Type.SCALAR)
        .setScalar(Value.Scalar.newBuilder().setValue(1)))
      .setExecutor(_executorInfo)
      .build();
  }

  def generateTaskInfos(offer: Offer): java.util.Collection[TaskInfo] = {
    val taskInfos = new java.util.ArrayList[TaskInfo]();
    taskInfos.add(createTaskInfo(offer.getSlaveId()));
    _nTasksCreated += 1;
    return taskInfos;
  }

  private var _nTasksTerminated = 0;

  def observeTaskStatusUpdate(taskStatus: TaskStatus) = {
    taskStatus.getState() match {
      case TaskState.TASK_FINISHED |
        TaskState.TASK_FAILED |
        TaskState.TASK_KILLED |
        TaskState.TASK_LOST => {
        _nTasksTerminated += 1;
      }
      case TaskState.TASK_STAGING |
        TaskState.TASK_STARTING |
        TaskState.TASK_RUNNING =>
    }
  }

  def done(): Boolean = {
    return _nTasksTerminated >= _nTasksCreated && doneCreatingTasks();
  }

}
