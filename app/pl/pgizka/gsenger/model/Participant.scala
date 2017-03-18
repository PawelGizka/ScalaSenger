package pl.pgizka.gsenger.model

import java.time.Instant

case class ParticipantId(value: Long) extends EntityId(value)

case class Participant(
  id: Option[ParticipantId],
  version: Option[Version],
  created: Option[Instant],
  modified: Option[Instant],

  user: Long,
  conversation: Long,
  lastViewedMessage: Long,
  messageViewedDate: Long) extends Entity[ParticipantId]
