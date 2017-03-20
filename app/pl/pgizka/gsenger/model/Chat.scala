package pl.pgizka.gsenger.model

import java.time.Instant

case class ChatId(value: Long) extends EntityId(value)

case class Chat(
  id: Option[ChatId],
  version: Option[Version],
  created: Option[Instant],
  modified: Option[Instant],

  chatType: String,
  name: Option[String],
  started: Instant) extends Entity[ChatId]
