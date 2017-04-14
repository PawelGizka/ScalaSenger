package scala.controllers

import pl.pgizka.gsenger.controllers.message.{CreateMessageRequest, MessageController}
import pl.pgizka.gsenger.model.{ChatId, ChatType, Message}
import play.api.libs.json.JsValue
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import scala.Utils._

import scala.concurrent.Future


class MessageControllerSpec extends ControllerSpecWithDefaultScenario {

  val messageController = new MessageController(this)

  import profile.api._
  import scala.data.DefaultScenario._

  "createMessage" should {
    "return 404 when there is no chat with specified id" in {
      val createMessageRequest = CreateMessageRequest(ChatId(100), "some message text")
      val request = FakeRequest().withHeaders("accessToken" -> token1.token).withBody(Json.toJson(createMessageRequest))

      val response = messageController.createMessage.apply(request)

      status(response) must equal (404)
    }

    "return 403 when user is not participant in specified chat" in {
      val createMessageRequest = CreateMessageRequest(chat1.id.get, "some message text")
      val request = FakeRequest().withHeaders("accessToken" -> token3.token).withBody(Json.toJson(createMessageRequest))

      val response = messageController.createMessage.apply(request)

      status(response) must equal (403)
    }

    "return 200 and newly create message" in {
      val messageText = "some message text"
      val createMessageRequest = CreateMessageRequest(chat1.id.get, messageText)
      val request = FakeRequest().withHeaders("accessToken" -> token1.token).withBody(Json.toJson(createMessageRequest))

      val response = messageController.createMessage.apply(request)

      status(response) must equal (200)
      val responseBody = contentAs[Message](response)
      responseBody.text must equal(messageText)
    }

  }

}
