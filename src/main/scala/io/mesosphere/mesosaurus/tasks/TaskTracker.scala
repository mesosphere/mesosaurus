package io.mesosphere.mesosaurus.tasks

import io.mesosphere.mesosaurus.Timestamp

trait TaskTracker {

  private[this] var history = Map[String, TaskHistory]()

  def arrived(taskId: String, ts: Timestamp): Unit =
    history = history + (taskId -> TaskHistory(ts))

  def launched(taskId: String, ts: Timestamp): Unit =
    history.get(taskId).foreach { taskHistory =>
      val entry = taskId -> taskHistory.copy(launched = Some(ts))
      history = history + entry
    }

  def started(taskId: String, ts: Timestamp): Unit =
    history.get(taskId).foreach { taskHistory =>
      val entry = taskId -> taskHistory.copy(started = Some(ts))
      history = history + entry
    }

  def finished(taskId: String, ts: Timestamp): Unit =
    history.get(taskId).foreach { taskHistory =>
      val entry = taskId -> taskHistory.copy(finished = Some(ts))
      history = history + entry
    }

  def history(taskId: String): Option[TaskHistory] =
    history.get(taskId)

  def history(): Map[String, TaskHistory] = history

}

object TaskTracker extends TaskTracker
