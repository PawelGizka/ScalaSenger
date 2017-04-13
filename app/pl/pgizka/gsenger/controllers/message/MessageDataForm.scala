package pl.pgizka.gsenger.controllers.message

import pl.pgizka.gsenger.Utils.Js
import pl.pgizka.gsenger.model.ChatId
import play.api.libs.json.Json


case class CreateMessageRequest(chatId: ChatId, text: String)

object CreateMessageRequest {
  implicit val createMessageRequestFormat: Js[CreateMessageRequest] = Json.format[CreateMessageRequest]
}

