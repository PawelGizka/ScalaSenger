package pl.pgizka.gsenger.actors

import pl.pgizka.gsenger.Utils.{formatSequenceMessage, getNotFoundElements}
import pl.pgizka.gsenger.actors.ChatManagerActor.{ChatCreated, CreateNewChat, CreateNewChatResponse}
import pl.pgizka.gsenger.errors.CouldNotFindUsersError
import pl.pgizka.gsenger.model._
import pl.pgizka.gsenger.persistance.DatabaseSupport
import pl.pgizka.gsenger.persistance.impl.DAL
import pl.pgizka.gsenger.startup.InitialData
import pl.pgizka.gsenger.actors.ActorsUtils.handleDbComplete
import pl.pgizka.gsenger.dtos.chats.{CreateChatRequestDto}

import scala.concurrent.Future

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Status}

object ChatManagerActor {

  def props(dataAccess: DAL with DatabaseSupport,
            initialData: InitialData): Props = Props(classOf[ChatManagerActor], dataAccess, initialData)

  case class CreateNewChat(createChatRequest: CreateChatRequestDto, userId: UserId, requestContext: RequestContext = RequestContext())

  //responses
  case class CreateNewChatResponse(chat: Chat,
                                   participants: Seq[Participant],
                                   override val requestContext: RequestContext) extends ActorResponse

  private case class ChatsLoaded(chats: Seq[Chat])
  private case class ChatCreated(chat: Chat, participants: Seq[Participant],
                                 createChatRequest: CreateChatRequestDto,
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
    case CreateNewChat(createChatRequest, userId, requestContext) =>
      val sender = context.sender() //cache sender in val in order to not close over context.sender()

      // wrapper for chat and participants because non-variable type argument
      // is unchecked since it is eliminated by erasure
      case class ChatWithParticipants(chat: Chat, participants: Seq[Participant])

      db.run(users.find(createChatRequest.participants)).flatMap{foundUsers =>
        val allSpecifiedUsersExists = foundUsers.size == createChatRequest.participants.size
        if (allSpecifiedUsersExists) {
          db.run(chats.insertFromRequest(createChatRequest, userId)).map(ChatWithParticipants.tupled(_))
        } else {
          val notFoundIds = getNotFoundElements(createChatRequest.participants, foundUsers.map(_.id.get))
          val errorMessage = formatSequenceMessage("Not found users ids: ", notFoundIds)
          Future(CouldNotFindUsersError(errorMessage))
        }
      } onComplete handleDbComplete(sender, requestContext) {
        case ChatWithParticipants(chat, participants) =>
          self ! ChatCreated(chat, participants, createChatRequest, sender, requestContext)
      }

    case ChatCreated(chat, participants, createChatRequest, sender, requestContext) =>
      val chatActor = createChatActor(chat, participants, Seq())
      chatActors = chatActors + ((chat.id.get, chatActor))
      sender ! CreateNewChatResponse(chat, participants, requestContext)

      participants.foreach{participant =>
        UserActor.actorSelection(participant.user) ! UserActor.AddedToChat(chat, participants)
      }
  }

  private def createChatActor(chat: Chat,
                              participants: Seq[Participant],
                              messages: Seq[Message]) =

    context.actorOf(ChatActor.props(chat.id.get, dataAccess, participants, messages), ChatActor.actorName(chat.id.get))


}
