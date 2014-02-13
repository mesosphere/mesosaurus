package io.mesosphere.mesosaurus

import org.apache.mesos.Protos._

/**
  * Generates infos for a fixed number of small tasks, one for each offer received.
  */
class SimpleTaskSource(requestedTasks :Int) extends TaskSource(requestedTasks) {

  private var createdTasks = 0;

  def doneCreatingTasks(): Boolean = {
    return createdTasks >= requestedTasks
  }

  private def createTaskInfo(slaveID: SlaveID): TaskInfo = {
    val taskID = TaskID.newBuilder()
      .setValue(Integer.toString(createdTasks))
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
    createdTasks += 1;
    return taskInfos;
  }

}
