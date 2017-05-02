package scala.controllers

import akka.actor.{ActorRef, ActorSystem}
import pl.pgizka.gsenger.Utils.formatSequenceMessage
import pl.pgizka.gsenger.actors.{ChatManagerActor, UserManagerActor}
import pl.pgizka.gsenger.controllers.chat.{ChatController, CreateChatRequest, CreateChatResponse, ListAllChatsWithParticipantInfoResponse}
import pl.pgizka.gsenger.errors._
import pl.pgizka.gsenger.model.{Chat, ChatId, ChatType}
import pl.pgizka.gsenger.services.facebook.realFacebookService
import pl.pgizka.gsenger.startup.InitialData
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{AnyContentAsEmpty, Request, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._

import scala.Utils._
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Awaitable, Future}


class ChatsControllerSpec extends ControllerSpecWithDefaultScenario {
  implicit lazy val system = ActorSystem()

  import profile.api._
  import pl.pgizka.gsenger.startup.DefaultScenario._

  var chatManager: ActorRef = _
  var userManager: ActorRef = _

  var chatsController: ChatController = _

  before {
    db.run(createDefaultScenarioAction).futureValue

    val initialData = await(InitialData.load(this))

    chatManager = system.actorOf(ChatManagerActor.props(this, initialData), "chatManager")
    userManager = system.actorOf(UserManagerActor.props(this, initialData, realFacebookService, chatManager), "userManager")

    chatsController = new ChatController(this, system, chatManager)
  }

  def await[T](awaitable: Awaitable[T]): T = {
    Await.result(awaitable, Duration.Inf)
  }

  after {
    db.run(schema.drop).futureValue
    system.stop(chatManager)
    system.stop(userManager)
  }


  "listAllChatsWithParticipantInfo" should {
    "return 200 and list of all chats with Participant info for specified user" in {
      val request = FakeRequest().withHeaders("accessToken" -> token1.token)
      val response: Future[Result] = chatsController.listAllChatsWithParticipantInfo.apply(request)

      status(response) must equal(200)

      val responseBody = contentAs[ListAllChatsWithParticipantInfoResponse](response)

      responseBody.chats must have size 2

      responseBody.chats.foreach{chatInfo =>
        chatInfo.id.value must (equal(1) or equal(2))
        chatInfo.participantsInfos must have size 2
      }
    }
  }

  "createChat" should {
    "return 200 and newly created chat id" in {
      val createChatRequest = CreateChatRequest(ChatType.groupChat, None, Seq(user2.id.get.value, user3.id.get.value))

      val request = FakeRequest().withHeaders("accessToken" -> token1.token).withBody(Json.toJson(createChatRequest))
      val response = chatsController.createChat.apply(request)

      status(response) must equal(200)

      val responseBody = contentAs[Chat](response)
      responseBody.id.get must equal(ChatId(3))
    }

    "return 400 and error response with detail info when there is no users with specified ids" in {
      val userIdWhichNotExist = 40
      val createChatRequest = CreateChatRequest(ChatType.groupChat, None, Seq(user2.id.get.value, user3.id.get.value, userIdWhichNotExist))

      val request = FakeRequest().withHeaders("accessToken" -> token1.token).withBody(Json.toJson(createChatRequest))
      val response = chatsController.createChat.apply(request)

      status(response) must equal(400)

      val responseBody = contentAsErrorResponse(response)
      responseBody.code must equal(CouldNotFindUsersError.code)
      responseBody.additionalInfo.get must equal(formatSequenceMessage("Not found users ids: ", Seq(userIdWhichNotExist)))
    }
  }

}
