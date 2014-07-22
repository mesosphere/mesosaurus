package io.mesosphere.mesosaurus.tasks

import io.mesosphere.mesosaurus.Timestamp

case class TaskHistory(
  arrived: Timestamp,
  launched: Option[Timestamp] = None,
  started: Option[Timestamp] = None,
  finished: Option[Timestamp] = None)
