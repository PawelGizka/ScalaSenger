package pl.pgizka.gsenger.model

import java.time.Instant

case class MessageId(value: Long) extends EntityId(value)

case class Message(
  id: Option[MessageId],
  version: Option[Version],
  created: Option[Instant],
  modified: Option[Instant],

  chat: ChatId,
  sender: UserId,
  number: Long,
  text: String) extends Entity[MessageId]
