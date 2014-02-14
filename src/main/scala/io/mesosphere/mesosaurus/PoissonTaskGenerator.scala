package io.mesosphere.mesosaurus

import org.apache.mesos.Protos._

/**
 * Generates tasks with poisson-distributed arrival time.
 */
class PoissonTaskGenerator(requestedTasks: Int, meanArrivalMilliseconds: Int, offerAttempts: Int = 100) extends TaskGenerator(requestedTasks) {

	private var _createdTasks = 0
	private var _forfeitedTasks = 0

	def doneCreatingTasks(): Boolean = {
		return _createdTasks + _forfeitedTasks >= requestedTasks
	}

	private def createTaskInfo(slaveID: SlaveID, resources: Resources): TaskInfo = {
		val taskID = TaskID.newBuilder()
			.setValue(Integer.toString(_createdTasks))
			.build()
		System.out.println("Launching task " + taskID.getValue())
		return TaskInfo.newBuilder()
			.setName("task " + taskID.getValue())
			.setTaskId(taskID)
			.setSlaveId(slaveID)
			.addResources(Resource.newBuilder()
				.setName("cpus")
				.setType(Value.Type.SCALAR)
				.setScalar(Value.Scalar.newBuilder().setValue(resources.cpus)))
			.addResources(Resource.newBuilder()
				.setName("mem")
				.setType(Value.Type.SCALAR)
				.setScalar(Value.Scalar.newBuilder().setValue(resources.mem)))
			.setExecutor(_executorInfo)
			.build()
	}

	private class TaskDescriptor(val arrivalTime: Int, val durationMilliSeconds: Int, val resources: Resources) extends Logging {
		var offerAttempts = 0

		def print() :Unit = {
			log.info("arrival: " + arrivalTime +
					", duration: " + durationMilliSeconds +
					", cpus : " + resources.cpus +
					", mem: " + resources.mem)
		}
	}

	private val _taskDescriptors = new Ring[TaskDescriptor](null)

	private def prepareTasks() = {
		val random = new PoissonRandom(meanArrivalMilliseconds)
		
		var arrivalMilliSeconds = 0
		for (i <- 0 until requestedTasks) {
			arrivalMilliSeconds += random.next().toInt
			val durationMilliSeconds = 2000 // TODO
			val resources = new Resources(1, 100) // TODO
			val taskDescriptor = new TaskDescriptor(arrivalMilliSeconds, durationMilliSeconds, resources)
			_taskDescriptors.add(taskDescriptor)
			taskDescriptor.print()
		}
	}

	prepareTasks()

	private var _startTime = System.currentTimeMillis

	override def start() = {
		_startTime = System.currentTimeMillis
	}

	def generateTaskInfos(offer: Offer): java.util.Collection[TaskInfo] = {
		var offerResources = new Resources(offer)
		val taskInfos = new java.util.ArrayList[TaskInfo]()
		val currentRunTime = System.currentTimeMillis - _startTime
		var t = _taskDescriptors.next
		while (t != _taskDescriptors && t.value.arrivalTime <= currentRunTime) {
			if (t.value.resources <= offerResources) {
				taskInfos.add(createTaskInfo(offer.getSlaveId(), t.value.resources))
				offerResources = offerResources - t.value.resources
				_createdTasks += 1
				t = t.remove()
			} else {
				if (t.value.offerAttempts >= offerAttempts) {
					_forfeitedTasks += 1
					t = t.remove()
				} else {
					t.value.offerAttempts += 1
					t = t.next
				}
			}
		}
		return taskInfos
	}
}

