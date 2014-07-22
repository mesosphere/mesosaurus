package io.mesosphere.mesosaurus.tasks

import io.mesosphere.mesosaurus.Timestamp

case class TaskHistory(
  arrived: Timestamp,
  launched: Option[Timestamp],
  started: Option[Timestamp],
  finished: Option[Timestamp])