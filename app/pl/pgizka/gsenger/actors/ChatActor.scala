package pl.pgizka.gsenger.actors

import pl.pgizka.gsenger.actors.ChatActor._
import pl.pgizka.gsenger.controllers.message.CreateMessageRequest
import pl.pgizka.gsenger.errors._
import pl.pgizka.gsenger.model.{ChatId, Message, Participant, UserId}
import pl.pgizka.gsenger.persistance.DatabaseSupport
import pl.pgizka.gsenger.persistance.impl.DAL
import pl.pgizka.gsenger.actors.ActorsUtils.handleDbComplete

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSelection, ActorSystem, Props}

import scala.concurrent.Future
import scala.util.{Failure, Success}

object ChatActor {

  def props(chatId: ChatId,
            dataAccess: DAL with DatabaseSupport,
            participantsLoaded: Seq[Participant],
            messagesLoaded: Seq[Message]): Props = Props(classOf[ChatActor], chatId, dataAccess, participantsLoaded, messagesLoaded)

  def actorName(chatId: ChatId): String = s"chat-$chatId"

  def actorSelection(chatId: ChatId)(implicit actorSystem: ActorSystem): ActorSelection =
    actorSystem.actorSelection(s"/user/chatManager/${actorName(chatId)}")

  case class CreateNewMessage(sender: UserId, createMessageRequest: CreateMessageRequest, requestContext: RequestContext = RequestContext())

  //responses
  case class CreateNewMessageResponse(message: Message, override val requestContext: RequestContext) extends ActorResponse

  private case class MessageCreated(message: Message, sender: ActorRef, requestContext: RequestContext = RequestContext())
}


class ChatActor(chatId: ChatId, dataAccess:
                DAL with DatabaseSupport,
                participantsLoaded: Seq[Participant],
                messagesLoaded: Seq[Message]) extends Actor with ActorLogging {

  import dataAccess._
  import profile.api._

  implicit val executionContext = context.dispatcher

  var participantsMap: Map[UserId, Participant] = Map()
  var messagesList: List[Message] = List()

  @scala.throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    participantsMap = Map(participantsLoaded.map(participant => (participant.user, participant)): _*)
    messagesList = messagesLoaded.toList
  }

  override def receive: Receive = {

    case CreateNewMessage(messageSender, createMessageRequest, requestContext) =>
      val sender = context.sender()
      db.run(participants.isUserParticipant(createMessageRequest.chatId, messageSender)).flatMap{hasAccess =>
        if (hasAccess) {
          db.run(messages.insert(createMessageRequest.chatId, messageSender, createMessageRequest.text))
        } else {
          Future(Forbidden)
        }
      } onComplete handleDbComplete(sender, requestContext){
        case message: Message => self ! MessageCreated(message, sender, requestContext)
      }

    case MessageCreated(message, sender, requestContext) =>
      messagesList = message :: messagesList
      sender ! CreateNewMessageResponse(message, requestContext)
      participantsMap.foreach {
        case (userId: UserId, _) => UserActor.actorSelection(userId)(context.system) ! UserActor.NewMessage(message)
      }

  }
}
