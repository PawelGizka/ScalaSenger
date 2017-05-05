package scala.controllers

import pl.pgizka.gsenger.Utils.formatSequenceMessage
import pl.pgizka.gsenger.actors.{ChatManagerActor, UserManagerActor}
import pl.pgizka.gsenger.controllers.chat.ChatController
import pl.pgizka.gsenger.dtos.chats.{CreateChatRequestDto, GetAllChatsWithParticipantInfoDto}
import pl.pgizka.gsenger.errors._
import pl.pgizka.gsenger.model.{Chat, ChatId, ChatType, UserId}
import pl.pgizka.gsenger.services.facebook.realFacebookService
import pl.pgizka.gsenger.startup.InitialData
import pl.pgizka.gsenger.startup.DefaultScenario._

import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._

import scala.Utils._
import scala.concurrent.Future

import akka.actor.ActorRef

class ChatsControllerSpec extends ControllerSpec {

  var chatManager: ActorRef = _
  var userManager: ActorRef = _

  var chatsController: ChatController = _

  override def onBefore(initialData: InitialData): Unit = {
    chatManager = system.actorOf(ChatManagerActor.props(this, initialData), "chatManager")
    userManager = system.actorOf(UserManagerActor.props(this, initialData, realFacebookService), "userManager")

    chatsController = new ChatController(this, system, chatManager)
  }

  override def onAfter(): Unit = {
    system.stop(chatManager)
    system.stop(userManager)
  }

  "listAllChatsWithParticipantInfo" should {
    "return 200 and list of all chats with Participant info for specified user" in {
      val request = FakeRequest().withHeaders("accessToken" -> token1.token)
      val response: Future[Result] = chatsController.getAllChatsWithParticipantInfo.apply(request)

      status(response) must equal(200)

      val responseBody = contentAs[GetAllChatsWithParticipantInfoDto](response)

      responseBody.chats must have size 2

      responseBody.chats.foreach{chatInfo =>
        chatInfo.id.value must (equal(1) or equal(2))
        chatInfo.participantsInfos must have size 2
      }
    }
  }

  "createChat" should {
    "return 200 and newly created chat id" in {
      val createChatRequest = CreateChatRequestDto(ChatType.groupChat, None, Seq(user2.id.get, user3.id.get))

      val request = FakeRequest().withHeaders("accessToken" -> token1.token).withBody(Json.toJson(createChatRequest))
      val response = chatsController.createChat.apply(request)

      status(response) must equal(200)

      val responseBody = contentAs[Chat](response)
      responseBody.id.get must equal(ChatId(3))
    }

    "return 400 and error response with detail info when there is no users with specified ids" in {
      val userIdWhichNotExist = UserId(40)
      val createChatRequest = CreateChatRequestDto(ChatType.groupChat, None, Seq(user2.id.get, user3.id.get, userIdWhichNotExist))

      val request = FakeRequest().withHeaders("accessToken" -> token1.token).withBody(Json.toJson(createChatRequest))
      val response = chatsController.createChat.apply(request)

      status(response) must equal(400)

      val responseBody = contentAsErrorResponse(response)
      responseBody.code must equal(CouldNotFindUsersError.code)
      responseBody.additionalInfo.get must equal(formatSequenceMessage("Not found users ids: ", Seq(userIdWhichNotExist)))
    }
  }

}
