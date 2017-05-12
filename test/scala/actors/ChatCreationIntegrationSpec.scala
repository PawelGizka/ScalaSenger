package scala.actors

import java.util.concurrent.TimeUnit

import pl.pgizka.gsenger.actors._
import pl.pgizka.gsenger.services.facebook.realFacebookService
import pl.pgizka.gsenger.startup.InitialData
import pl.pgizka.gsenger.dtos.chats.{ChatDto, CreateChatRequestDto}
import pl.pgizka.gsenger.model.{ChatType, UserId}
import pl.pgizka.gsenger.startup.DefaultScenario.{user1, user2, user3}
import pl.pgizka.gsenger.startup.Implicits.akkAskTimeout
import akka.actor.ActorRef
import akka.testkit.{TestActor, TestKitBase, TestProbe}
import pl.pgizka.gsenger.errors
import play.api.libs.json.{JsValue, Json}
import play.api.libs.json.Json.toJson

import scala.concurrent.duration.FiniteDuration

class ChatCreationIntegrationSpec extends ActorSpec with TestKitBase {

  var chatManager: ActorRef = _
  var userManager: ActorRef = _

  var out1: TestProbe = _
  var out2: TestProbe = _
  var out3: TestProbe = _

  var webSocketActor1: ActorRef = _
  var webSocketActor2: ActorRef = _
  var webSocketActor3: ActorRef = _

  override def onBefore(initialData: InitialData): Unit = {
    chatManager = system.actorOf(ChatManagerActor.props(this, initialData), "chatManager")
    userManager = system.actorOf(UserManagerActor.props(this, initialData, realFacebookService), "userManager")
    Thread.sleep(500)

    val user1Actor = UserActor.actorSelection(user1.id.get).resolveOne().futureValue
    val user2Actor = UserActor.actorSelection(user2.id.get).resolveOne().futureValue
    val user3Actor = UserActor.actorSelection(user3.id.get).resolveOne().futureValue

    out1 = TestProbe("out1")
    out2 = TestProbe("out1")
    out3 = TestProbe("out1")

    webSocketActor1 = system.actorOf(WebSocketActor.props(out1.testActor, user1.id.get, user1Actor, chatManager, this))
    webSocketActor2 = system.actorOf(WebSocketActor.props(out2.testActor, user2.id.get, user1Actor, chatManager, this))
    webSocketActor3 = system.actorOf(WebSocketActor.props(out3.testActor, user3.id.get, user1Actor, chatManager, this))
  }

  override def onAfter(): Unit = {
    system.stop(chatManager)
    system.stop(userManager)
  }

  "create new chat" should {

    "return error message when specified users (users ids) not exists" in {
      val userIdWhichNotExist = UserId(40)
      val createChatRequest = CreateChatRequestDto(ChatType.groupChat, None, Seq(user2.id.get, user3.id.get, userIdWhichNotExist))
      val webSocketRequest = toJson(WebSocketRequest("createNewChat", Some("requestId"), toJson(createChatRequest)))

      webSocketActor1 ! webSocketRequest

      val errorResponse = out1.receiveOne(FiniteDuration(3, TimeUnit.MINUTES)).asInstanceOf[JsValue].as[WebSocketErrorResponse]

      errorResponse.status must equal(errors.CouldNotFindUsersError.code)
    }

    "create new chat," +
      "returned it to sender (WebSocketActor)," +
      "send information about adding to chat to all participants and all participants devices (WebSocketActors)" in {

      val createChatRequest = CreateChatRequestDto(ChatType.groupChat, None, Seq(user2.id.get, user3.id.get))
      val webSocketRequest = toJson(WebSocketRequest("createNewChat", Some("requestId"), toJson(createChatRequest)))

      webSocketActor1 ! webSocketRequest

      val response = out1.receiveOne(FiniteDuration(3, TimeUnit.MINUTES)).asInstanceOf[JsValue].as[WebSocketResponse]

      val responseChatDto = response.content.as[ChatDto]
      responseChatDto.participantsInfos must have size 3

      val push1 = out2.receiveOne(FiniteDuration(3, TimeUnit.MINUTES)).asInstanceOf[JsValue].as[WebSocketPush]
      push1.method must equal("addedToChat")

      val push1ChatDto = push1.content.as[ChatDto]
      push1ChatDto.participantsInfos must have size 3

      val push2 = out3.receiveOne(FiniteDuration(3, TimeUnit.MINUTES)).asInstanceOf[JsValue].as[WebSocketPush]
      push1.method must equal("addedToChat")

      val push2ChatDto = push1.content.as[ChatDto]
      push2ChatDto.participantsInfos must have size 3
    }

  }

}

