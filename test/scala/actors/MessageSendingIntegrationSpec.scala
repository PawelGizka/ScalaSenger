package scala.actors

import pl.pgizka.gsenger.actors._
import pl.pgizka.gsenger.dtos.messages.{CreateMessageRequestDto, MessageDto}
import pl.pgizka.gsenger.errors
import pl.pgizka.gsenger.model.ChatId
import pl.pgizka.gsenger.startup.DefaultScenario._

import play.api.libs.json.{JsValue, Json}

class MessageSendingIntegrationSpec extends ActorIntegrationSpec {


  "create new message" should {

    "create new message," +
      "returned it to sender (WebSocketActor," +
      "send newly created message to all participants in chat and all participants devices (WebSocketActors)" in {
      val messageText = "some message text"
      val createMessageRequest = CreateMessageRequestDto(chat1.id.get, messageText)
      val webSocketRequest = WebSocketRequest("createNewMessage", Some("requestId"), Json.toJson(createMessageRequest))

      webSocketActor1 ! Json.toJson(Json.toJson(webSocketRequest))

      val webSocketResponse = receiveMessage(out1).asInstanceOf[JsValue].as[WebSocketResponse]
      val responseMessageDto = webSocketResponse.content.as[MessageDto]
      responseMessageDto.text must equal(messageText)
      responseMessageDto.chat must equal(chat1.id.get)

      val webSocketPush = receiveMessage(out2).asInstanceOf[JsValue].as[WebSocketPush]
      webSocketPush.method must equal("newMessage")

      val pushDto = webSocketPush.content.as[MessageDto]
      pushDto.text must equal(messageText)
      pushDto.chat must equal(chat1.id.get)

      out3.expectNoMsg()
    }

    "return forbidden response when user is not a member of chat" in {
      val chatIdWhichNotExists = ChatId(100)
      val createMessageRequest = CreateMessageRequestDto(chatIdWhichNotExists, "some message text")
      val webSocketRequest = WebSocketRequest("createNewMessage", Some("requestId"), Json.toJson(createMessageRequest))

      webSocketActor1 ! Json.toJson(Json.toJson(webSocketRequest))

      val errorResponse = receiveMessage(out1).asInstanceOf[JsValue].as[WebSocketErrorResponse]

      errorResponse.status must equal(errors.CouldNotFindChatError.code)

    }
  }

}
