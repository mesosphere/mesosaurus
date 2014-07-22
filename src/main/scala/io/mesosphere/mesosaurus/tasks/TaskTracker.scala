package io.mesosphere.mesosaurus.tasks

import io.mesosphere.mesosaurus.Timestamp

trait TaskTracker {

  private[this] var history = Map[String, TaskHistory]()

  def arrived(taskId: String, ts: Timestamp): Unit = {
    ??? // TODO
  }

  def launched(taskId: String, ts: Timestamp): Unit = {
    ??? // TODO
  }

  def started(taskId: String, ts: Timestamp): Unit = {
    ??? // TODO
  }

  def finished(taskId: String, ts: Timestamp): Unit = {
    ??? // TODO
  }

  def history(taskId: String): Option[TaskHistory] =
    history.get(taskId)

  def history(): Map[String, TaskHistory] = history

}