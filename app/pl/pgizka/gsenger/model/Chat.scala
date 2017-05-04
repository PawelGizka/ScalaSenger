package pl.pgizka.gsenger.model

import java.time.Instant

import pl.pgizka.gsenger.Utils.Js
import pl.pgizka.gsenger.dtos.chats.CreateChatRequestDto
import play.api.libs.json.{Json, OFormat}

case class ChatId(value: Long) extends EntityId(value)

object ChatId {
  implicit val chatIdFormat: Js[ChatId] = Json.format[ChatId]
}

case class Chat(
  id: Option[ChatId],

  chatType: String,
  name: Option[String],
  started: Instant) extends Entity[ChatId] {

  def this(createChatRequest: CreateChatRequestDto) =
    this(
      None,

      createChatRequest.chatType,
      createChatRequest.name,
      Instant.now()
    )
}

object Chat {
  implicit val chatFormat: Js[Chat] = Json.format[Chat]
}

object ChatType {
  val singleChat = "singleChat"
  val groupChat = "groupChat"
}
