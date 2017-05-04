package pl.pgizka.gsenger.dtos.messages

import pl.pgizka.gsenger.Utils.Js
import pl.pgizka.gsenger.model.{ChatId, Message, MessageId, UserId}
import play.api.libs.json.Json


case class CreateMessageRequestDto(chatId: ChatId, text: String)

object CreateMessageRequestDto {
  implicit val createMessageRequestDtoFormat: Js[CreateMessageRequestDto] = Json.format[CreateMessageRequestDto]
}

case class MessageDto(id: MessageId, chat: ChatId, sender: UserId, number: Long, text: String) {
  def this(message: Message) = this(message.id.get, message.chat, message.sender, message.number, message.text)
}

object MessageDto {
  implicit val messageDtoFormat: Js[MessageDto] = Json.format[MessageDto]
}

