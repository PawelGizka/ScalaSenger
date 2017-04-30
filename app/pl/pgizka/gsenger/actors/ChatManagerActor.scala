package pl.pgizka.gsenger.actors

import pl.pgizka.gsenger.Utils.{formatSequenceMessage, getNotFoundElements}
import pl.pgizka.gsenger.actors.ChatManagerActor.{ChatCreated, CreateNewChat}
import pl.pgizka.gsenger.controllers.chat.{CreateChatRequest}
import pl.pgizka.gsenger.errors.{CouldNotFindUsersError, DatabaseError}
import pl.pgizka.gsenger.model._
import pl.pgizka.gsenger.persistance.DatabaseSupport
import pl.pgizka.gsenger.persistance.impl.DAL
import pl.pgizka.gsenger.startup.{InitialData, boot}
import pl.pgizka.gsenger.actors.ActorsUtils.handleDbComplete

import akka.pattern._
import akka.actor.{Actor, ActorLogging, ActorRef, Props, Status}

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}


object ChatManagerActor {

  def props(dataAccess: DAL with DatabaseSupport,
            initialData: InitialData): Props = Props(classOf[ChatManagerActor], dataAccess, initialData)

  case class CreateNewChat(createChatRequest: CreateChatRequest, user: User, requestContext: RequestContext = RequestContext())

  private case class ChatsLoaded(chats: Seq[Chat])
  private case class ChatCreated(chat: Chat, participants: Seq[Participant],
                                 createChatRequest: CreateChatRequest,
                                 sender: ActorRef,
                                 requestContext: RequestContext)
}

class ChatManagerActor(dataAccess: DAL with DatabaseSupport,
                       initialData: InitialData) extends Actor with ActorLogging {

  import dataAccess._
  import profile.api._

  private implicit val executionContext = context.dispatcher
  private implicit val actorSystem = context.system

  var chatActors: Map[ChatId, ActorRef] = _

  @scala.throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    val chatActorsSeq = initialData.chats.map{chat => {
      val chatId = chat.id.get
      val chatActorRef = createChatActor(chat, initialData.chatParticipantsMap(chatId), initialData.chatMessagesMap(chatId))
      (chatId, chatActorRef)
    }}

    chatActors = Map(chatActorsSeq: _*)
  }

  override def receive: Receive = {
    case CreateNewChat(createChatRequest, user, requestContext) =>
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
      } onComplete handleDbComplete(sender, requestContext) {
        case (chatWithParticipants: (Chat, Seq[Participant])) =>
          self ! ChatCreated(chatWithParticipants._1, chatWithParticipants._2, createChatRequest, sender, requestContext)
      }

    case ChatCreated(chat, participants, createChatRequest, sender, requestContext) =>
      val chatActor = createChatActor(chat, participants, Seq())
      chatActors = chatActors + ((chat.id.get, chatActor))
      sender ! ActorResponse((chat, participants), requestContext)

      participants.foreach{participant =>
        UserActor.actorSelection(participant.user) ! UserActor.AddedToChat(chat, participants)
      }
  }

  private def createChatActor(chat: Chat,
                              participants: Seq[Participant],
                              messages: Seq[Message]) =

    context.actorOf(ChatActor.props(chat.id.get, dataAccess, participants, messages), ChatActor.actorName(chat.id.get))


}
