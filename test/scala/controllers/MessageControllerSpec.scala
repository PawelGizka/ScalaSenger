package scala.controllers

import pl.pgizka.gsenger.actors.{ChatManagerActor, UserManagerActor}
import pl.pgizka.gsenger.controllers.message.MessageController
import pl.pgizka.gsenger.dtos.messages.CreateMessageRequestDto
import pl.pgizka.gsenger.model.{ChatId, Message}
import pl.pgizka.gsenger.services.facebook.realFacebookService
import pl.pgizka.gsenger.startup.DefaultScenario._
import pl.pgizka.gsenger.startup.InitialData
import play.api.test.FakeRequest
import play.api.libs.json.Json
import play.api.test.Helpers._

import scala.Utils._
import akka.actor.ActorRef

class MessageControllerSpec extends ControllerSpec {

  var chatManager: ActorRef = _
  var userManager: ActorRef = _

  var messageController: MessageController = _

  override def onBefore(initialData: InitialData): Unit = {
    chatManager = system.actorOf(ChatManagerActor.props(this, initialData), "chatManager")
    userManager = system.actorOf(UserManagerActor.props(this, initialData, realFacebookService, chatManager), "userManager")

    messageController = new MessageController(this, system)
  }

  override def onAfter(): Unit = {
    system.stop(chatManager)
    system.stop(userManager)
  }

  "createMessage" should {
    "return 404 when there is no chat with specified id" in {
      val createMessageRequest = CreateMessageRequestDto(ChatId(100), "some message text")
      val request = FakeRequest().withHeaders("accessToken" -> token1.token).withBody(Json.toJson(createMessageRequest))

      val response = messageController.createMessage.apply(request)

      status(response) must equal (404)
    }

    "return 400 when user is not participant in specified chat" in {
      val createMessageRequest = CreateMessageRequestDto(chat1.id.get, "some message text")
      val request = FakeRequest().withHeaders("accessToken" -> token3.token).withBody(Json.toJson(createMessageRequest))

      val response = messageController.createMessage.apply(request)

      status(response) must equal (400)
    }

    "return 200 and newly create message" in {
      val messageText = "some message text"
      val createMessageRequest = CreateMessageRequestDto(chat1.id.get, messageText)
      val request = FakeRequest().withHeaders("accessToken" -> token1.token).withBody(Json.toJson(createMessageRequest))

      val response = messageController.createMessage.apply(request)

      status(response) must equal (200)
      val responseBody = contentAs[Message](response)
      responseBody.text must equal(messageText)
    }

  }

}
