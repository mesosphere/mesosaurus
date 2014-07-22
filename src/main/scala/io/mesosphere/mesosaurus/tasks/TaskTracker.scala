package io.mesosphere.mesosaurus.tasks

import io.mesosphere.mesosaurus.Timestamp
import scala.collection.immutable.SortedSet

trait TaskTracker {

  private[this] var history = Map[String, TaskHistory]()

  case class Sample[T](value: T, timestamp: Timestamp)

  implicit val _: Ordering[Sample[Int]] =
    Ordering.by { (s: Sample[Int]) => s.timestamp }

  private[this] var queueSamples = SortedSet[Sample[Int]]()

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

  def queueLength(length: Int, ts: Timestamp): Unit =
    queueSamples = queueSamples + Sample(length, ts)

  def queueHistory(): SortedSet[Sample[Int]] = queueSamples

  def taskHistory(taskId: String): Option[TaskHistory] =
    history.get(taskId)

  def taskHistory(): Map[String, TaskHistory] = history

}

object TaskTracker extends TaskTracker
