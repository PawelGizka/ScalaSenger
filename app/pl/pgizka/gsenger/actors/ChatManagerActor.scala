package pl.pgizka.gsenger.actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Status}
import pl.pgizka.gsenger.model._
import pl.pgizka.gsenger.persistance.DatabaseSupport
import pl.pgizka.gsenger.persistance.impl.DAL
import pl.pgizka.gsenger.startup.boot
import akka.pattern._
import pl.pgizka.gsenger.Utils.{formatSequenceMessage, getNotFoundElements}
import pl.pgizka.gsenger.actors.ChatManagerActor.{ChatCreated, ChatsLoaded, CreateNewChat}
import pl.pgizka.gsenger.controllers.RestApiErrorResponse
import pl.pgizka.gsenger.controllers.chat.{CreateChatRequest, CreateChatResponse}
import pl.pgizka.gsenger.errors.{CouldNotFindUsersError, DatabaseError}

import scala.concurrent.Future
import scala.util.{Failure, Success}


object ChatManagerActor {

  def props(dataAccess: DAL with DatabaseSupport): Props = Props(classOf[ChatManagerActor], dataAccess)

  case class CreateNewChat(createChatRequest: CreateChatRequest, user: User)

  private case class ChatsLoaded(chats: Seq[Chat])
  private case class ChatCreated(chat: Chat, createChatRequest: CreateChatRequest, sender: ActorRef)
}

class ChatManagerActor(dataAccess: DAL with DatabaseSupport) extends Actor with ActorLogging {

  import dataAccess._
  import profile.api._

  private implicit val executionContext = context.dispatcher

  var chatActors: Map[ChatId, ActorRef] = _

  @scala.throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    db.run(chats.list()).map(ChatsLoaded) pipeTo self
  }

  override def receive: Receive = {
    case ChatsLoaded(chats) =>
      val chatActorsSeq = chats.map{chat => (chat.id.get, createChatActor(chat))}
      chatActors = Map(chatActorsSeq: _*)

    case CreateNewChat(createChatRequest, user) =>
      val sender = context.sender() //cache sender in val in order to not close over context.sender()
      db.run(users.find(createChatRequest.participants.map(new UserId(_)))).flatMap{foundUsers =>
        val allSpecifiedUsersExists = foundUsers.size == createChatRequest.participants.size
        if (allSpecifiedUsersExists) {
          db.run(chats.insertFromRequest(createChatRequest, user))
        } else {
          val notFoundIds = getNotFoundElements(createChatRequest.participants, foundUsers.map(_.id.get.value))
          val errorMessage = formatSequenceMessage("Not found users ids: ", notFoundIds)
          Future(CouldNotFindUsersError(errorMessage))
        }
      } onComplete {
        case Success(chat: Chat) ⇒ self ! ChatCreated(chat, createChatRequest, sender)
        case Success(error) => sender ! error
        case Failure(throwable) => sender ! DatabaseError(throwable.getMessage)
      }

    case ChatCreated(chat, createChatRequest, sender) =>
      val chatActor = createChatActor(chat)
      chatActors = chatActors + ((chat.id.get, chatActor))
      //TODO send addedToChat message to all participants
      sender ! chat
  }

  private def createChatActor(chat: Chat) =
    //TODO provide all necessary arguments for chat initialization
    context.actorOf(ChatActor.props(chat.id.get, dataAccess), ChatActor.actorName(chat.id.get))


}
