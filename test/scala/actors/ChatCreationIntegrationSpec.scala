package scala.actors

import pl.pgizka.gsenger.actors._
import pl.pgizka.gsenger.dtos.chats.{ChatDto, CreateChatRequestDto}
import pl.pgizka.gsenger.model.{ChatType, UserId}
import pl.pgizka.gsenger.startup.DefaultScenario.{user2, user3}
import pl.pgizka.gsenger.errors

import play.api.libs.json.JsValue
import play.api.libs.json.Json.toJson

class ChatCreationIntegrationSpec extends ActorIntegrationSpec {

  "create new chat" should {

    "return error message when specified users (users ids) not exists" in {
      val userIdWhichNotExist = UserId(40)
      val createChatRequest = CreateChatRequestDto(ChatType.groupChat, None, Seq(user2.id.get, user3.id.get, userIdWhichNotExist))
      val webSocketRequest = toJson(WebSocketRequest("createNewChat", Some("requestId"), toJson(createChatRequest)))

      webSocketActor1 ! webSocketRequest

      val errorResponse = receiveMessage(out1).asInstanceOf[JsValue].as[WebSocketErrorResponse]

      errorResponse.status must equal(errors.CouldNotFindUsersError.code)
    }

    "create new chat," +
      "returned it to sender (WebSocketActor)," +
      "send information about adding to chat to all participants and all participants devices (WebSocketActors)" in {

      val createChatRequest = CreateChatRequestDto(ChatType.groupChat, None, Seq(user2.id.get, user3.id.get))
      val webSocketRequest = toJson(WebSocketRequest("createNewChat", Some("requestId"), toJson(createChatRequest)))

      webSocketActor1 ! webSocketRequest

      val response = receiveMessage(out1).asInstanceOf[JsValue].as[WebSocketResponse]

      val responseChatDto = response.content.as[ChatDto]
      responseChatDto.participantsInfos must have size 3

      val push1 = receiveMessage(out2).asInstanceOf[JsValue].as[WebSocketPush]
      push1.method must equal("addedToChat")

      val push1ChatDto = push1.content.as[ChatDto]
      push1ChatDto.participantsInfos must have size 3

      val push2 = receiveMessage(out3).asInstanceOf[JsValue].as[WebSocketPush]
      push1.method must equal("addedToChat")

      val push2ChatDto = push1.content.as[ChatDto]
      push2ChatDto.participantsInfos must have size 3
    }

  }

}

