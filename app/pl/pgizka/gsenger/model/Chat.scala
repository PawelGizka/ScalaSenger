package pl.pgizka.gsenger.model

import java.time
import java.time.Instant

import pl.pgizka.gsenger.Utils.Js
import pl.pgizka.gsenger.core.CreateChatRequest
import play.api.libs.json.{Json, OFormat}

case class ChatId(value: Long) extends EntityId(value)

object ChatId {
  implicit val chatIdFormat: Js[ChatId] = Json.format[ChatId]
}

case class Chat(
  id: Option[ChatId],
  version: Option[Version],
  created: Option[Instant],
  modified: Option[Instant],

  chatType: String,
  name: Option[String],
  started: Instant) extends Entity[ChatId] {

  def this(createChatRequest: CreateChatRequest) =
    this(
      None,
      None,
      None,
      None,
      createChatRequest.chatType,
      createChatRequest.name,
      Instant.now()
    )
}

object ChatType {
  val singleChat = "singleChat"
  val groupChat = "groupChat"
}
