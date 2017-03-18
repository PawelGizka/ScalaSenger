package pl.pgizka.gsenger.model

import java.time.Instant

case class MessageId(value: Long) extends EntityId(value)

case class Message(
  id: Option[MessageId],
  version: Option[Version],
  created: Option[Instant],
  modified: Option[Instant],

  conversationId: Long,
  sender: Long,
  number: Long) extends Entity[MessageId]
