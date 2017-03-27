package pl.pgizka.gsenger.model

import java.time.Instant

import pl.pgizka.gsenger.Utils.Js
import play.api.libs.json.{Json, OFormat}

case class MessageId(value: Long) extends EntityId(value)

object MessageId {
  implicit val messageIdFormat: Js[MessageId] = Json.format[MessageId]
}

case class Message(
  id: Option[MessageId],
  version: Option[Version],
  created: Option[Instant],
  modified: Option[Instant],

  chat: ChatId,
  sender: UserId,
  number: Long,
  text: String) extends Entity[MessageId]
