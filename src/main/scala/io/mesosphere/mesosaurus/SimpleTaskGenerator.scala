package io.mesosphere.mesosaurus

import org.apache.mesos.Protos._

/**
  * Generates task infos for a fixed number of resource-light tasks, only one for each offer received.
  */
class SimpleTaskGenerator(requestedTasks: Int, taskDuration: Int) extends TaskGenerator(requestedTasks) {

  private var createdTasks = 0;

  def doneCreatingTasks(): Boolean = {
    return createdTasks >= requestedTasks
  }

  private def createTaskInfo(offer: Offer): TaskInfo = {
    val taskID = TaskID.newBuilder()
      .setValue(Integer.toString(createdTasks))
      .build();
    System.out.println("Launching task " + taskID.getValue());
    val offerResources = new Resources(offer)
    return TaskInfo.newBuilder()
      .setName("task " + taskID.getValue())
      .setTaskId(taskID)
      .setSlaveId(offer.getSlaveId())
      .addResources(Resource.newBuilder()
        .setName("cpus")
        .setType(Value.Type.SCALAR)
        .setScalar(Value.Scalar.newBuilder().setValue(offerResources.cpus)))
      .addResources(Resource.newBuilder()
        .setName("mem")
        .setType(Value.Type.SCALAR)
        .setScalar(Value.Scalar.newBuilder().setValue(offerResources.mem)))
      .setExecutor(_executorInfo)
      .build();
  }

  def generateTaskInfos(offer: Offer): java.util.Collection[TaskInfo] = {
    val taskInfos = new java.util.ArrayList[TaskInfo]();
    taskInfos.add(createTaskInfo(offer));
    createdTasks += 1;
    return taskInfos;
  }

}
