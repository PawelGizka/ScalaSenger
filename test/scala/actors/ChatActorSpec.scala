package scala.actors

import pl.pgizka.gsenger.actors.{ActorErrorResponse, ChatActor}
import pl.pgizka.gsenger.startup.InitialData
import pl.pgizka.gsenger.startup.DefaultScenario._
import pl.pgizka.gsenger.dtos.messages.CreateMessageRequestDto
import pl.pgizka.gsenger.startup.Implicits.akkAskTimeout
import pl.pgizka.gsenger.actors.ChatActor.{CreateNewMessage, CreateNewMessageResponse}
import pl.pgizka.gsenger.errors

import akka.testkit.TestActorRef
import akka.pattern.ask

import scala.concurrent.ExecutionContext.Implicits.global

class ChatActorSpec extends ActorSpec {

  var chatActor: TestActorRef[ChatActor] = _

  override def onBefore(initialData: InitialData) {
    val chatId = chat1.id.get

    chatActor = TestActorRef(new ChatActor(
      chatId = chatId,
      dataAccess = this,
      participantsLoaded = initialData.chatParticipantsMap(chatId),
      messagesLoaded = initialData.chatMessagesMap(chatId)))
  }

  override def onAfter() {
    chatActor.stop()
  }

  "createNewMessage" should {
    "return Forbidden when user is not a member of chat" in {
      val createMessageRequest = CreateMessageRequestDto(chat1.id.get, "some message text")

      chatActor ? CreateNewMessage(user3.id.get, createMessageRequest) map {response =>
        response mustBe an[ActorErrorResponse]

        val ActorErrorResponse(error, _) = response.asInstanceOf[ActorErrorResponse]

        error.code must equal(errors.Forbidden.code)
      }
    }

    "return newly inserted message when user is member of chat" in {
      val messageText = "some message text"
      val createMessageRequest = CreateMessageRequestDto(chat1.id.get, messageText)

      chatActor ? CreateNewMessage(user1.id.get, createMessageRequest) map {response =>
        response mustBe an[CreateNewMessageResponse]

        val CreateNewMessageResponse(message, _) = response.asInstanceOf[CreateNewMessageResponse]

        message.sender must equal(user1.id.get)
        message.text must equal(messageText)
      }
    }
  }

}
