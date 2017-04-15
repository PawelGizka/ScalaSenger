package pl.pgizka.gsenger.model

import pl.pgizka.gsenger.Utils.Js
import play.api.libs.json.{Json, OFormat}

case class MessageId(value: Long) extends EntityId(value)

object MessageId {
  implicit val messageIdFormat: Js[MessageId] = Json.format[MessageId]
}

case class Message(
  id: Option[MessageId],

  chat: ChatId,
  sender: UserId,
  number: Long,
  text: String) extends Entity[MessageId]{

  def this(chatId: ChatId, senderId: UserId, text: String, number: Long) =
    this(None, chatId, senderId, number, text)

}

object Message {
  implicit val messageFormat: Js[Message] = Json.format[Message]
}
