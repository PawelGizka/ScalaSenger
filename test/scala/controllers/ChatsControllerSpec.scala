package scala.controllers

import pl.pgizka.gsenger.Utils.formatSequenceMessage
import pl.pgizka.gsenger.core._
import pl.pgizka.gsenger.core.Error._
import pl.pgizka.gsenger.core.errors._
import pl.pgizka.gsenger.model.ChatType
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._

import scala.Utils._
import scala.concurrent.Future


class ChatsControllerSpec extends ControllerSpec {

  val chatsController = new ChatsController(this)

  import profile.api._

  val (user1, token1) = testUserWithToken(1)
  val (user2, token2) = testUserWithToken(2)
  val (user3, token3) = testUserWithToken(3)

  val chat1 = testChat(1)
  val chat2 = testChat(2)

  val participant1 = testParticipant(user1.id.get, chat1.id.get)
  val participant2 = testParticipant(user2.id.get, chat1.id.get)

  val participant3 = testParticipant(user1.id.get, chat2.id.get)
  val participant4 = testParticipant(user3.id.get, chat2.id.get)

  val userTestData = List(user1, user2, user3)
  val tokenTestData = List(token1, token2, token3)
  val chatTestData = List(chat1, chat2)
  val participantTestData = List(participant1, participant2, participant3, participant4)

  before {
    db.run(DBIO.seq(
      schema.create,
      users ++= userTestData,
      tokens ++= tokenTestData,
      chats ++= chatTestData,
      participants ++= participantTestData
    )).futureValue
  }

  after {
    db.run(drop()).futureValue
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

      val responseBody = contentAs[CreateChatResponse](response)
      responseBody.chatId must equal(3)
    }

    "return 400 and error response wiht detail info when there is no users with specified ids" in {
      val userIdWhichNotExist = 4
      val createChatRequest = CreateChatRequest(ChatType.groupChat, None, Seq(user2.id.get.value, user3.id.get.value, userIdWhichNotExist))

      val request = FakeRequest().withHeaders("accessToken" -> token1.token).withBody(Json.toJson(createChatRequest))
      val response = chatsController.createChat.apply(request)

      status(response) must equal(400)

      val responseBody = contentAsErrorResponse(response)
      responseBody.code must equal(CouldNotFindUsersError.code)
      responseBody.additionalInfo.get must equal(formatSequenceMessage("Not found users ids: ", Seq(4)))
    }
  }

}
