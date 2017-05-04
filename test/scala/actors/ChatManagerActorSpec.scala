package scala.actors

import pl.pgizka.gsenger.actors.{ActorErrorResponse, ChatManagerActor}
import pl.pgizka.gsenger.dtos.chats.CreateChatRequestDto
import pl.pgizka.gsenger.model.{ChatType, UserId}
import pl.pgizka.gsenger.startup.InitialData
import pl.pgizka.gsenger.actors.ChatManagerActor.CreateNewChatResponse
import pl.pgizka.gsenger.startup.Implicits.akkAskTimeout
import pl.pgizka.gsenger.startup.DefaultScenario._
import pl.pgizka.gsenger.errors

import akka.testkit.TestActorRef
import akka.pattern.ask

import scala.concurrent.ExecutionContext.Implicits.global

class ChatManagerActorSpec extends ActorTestSpec {

  var chatManagerActor: TestActorRef[ChatManagerActor] = _

  override def onBefore(initialData: InitialData) {

    chatManagerActor = TestActorRef(new ChatManagerActor(this, initialData))
  }

  override def onAfter() {

    chatManagerActor.stop()
  }

  "createNewChat" should {
    "return newly created chat" in {
      val createChatRequest = CreateChatRequestDto(ChatType.groupChat, None, Seq(user2.id.get, user3.id.get))

      chatManagerActor ? ChatManagerActor.CreateNewChat(createChatRequest, user1.id.get) map {message =>
        message mustBe an[CreateNewChatResponse]

        val CreateNewChatResponse(chat, participants, _) = message.asInstanceOf[CreateNewChatResponse]
        chat.chatType must equal(ChatType.groupChat)
        participants must have size 3
      }
    }

    "return error response when there is not enough users" in {
      val userIdWhichNotExist = UserId(40)
      val createChatRequest = CreateChatRequestDto(ChatType.groupChat, None, Seq(user2.id.get, user3.id.get, userIdWhichNotExist))

      chatManagerActor ? ChatManagerActor.CreateNewChat(createChatRequest, user1.id.get) map {message =>
        message mustBe an[ActorErrorResponse]

        val ActorErrorResponse(error, _) = message.asInstanceOf[ActorErrorResponse]

        error.code must equal(errors.CouldNotFindUsersError.code)
      }
    }
  }

}
