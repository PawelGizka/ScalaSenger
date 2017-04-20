package pl.pgizka.gsenger.actors

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSelection, ActorSystem, Props}
import pl.pgizka.gsenger.actors.ChatActor._
import pl.pgizka.gsenger.controllers.message.CreateMessageRequest
import pl.pgizka.gsenger.errors._
import pl.pgizka.gsenger.model.{ChatId, Message, Participant, UserId}
import pl.pgizka.gsenger.persistance.DatabaseSupport
import pl.pgizka.gsenger.persistance.impl.DAL

import scala.concurrent.Future
import scala.util.{Failure, Success}

object ChatActor {

  def props(chatId: ChatId, dataAccess: DAL with DatabaseSupport): Props = Props(classOf[ChatActor], chatId, dataAccess)
  def actorName(chatId: ChatId): String = s"chat-$chatId"

  def actorSelection(chatId: ChatId)(implicit actorSystem: ActorSystem): ActorSelection =
    actorSystem.actorSelection(s"user/chats/${actorName(chatId)}")

  case class CreateNewMessage(sender: UserId, createMessageRequest: CreateMessageRequest)

  private case class MessageCreated(message: Message, sender: ActorRef)
}


class ChatActor(chatId: ChatId, dataAccess:
                DAL with DatabaseSupport,
                participantsLoaded: Map[ChatId, Seq[Participant]],
                messagesLoaded: Map[ChatId, Seq[Message]]) extends Actor with ActorLogging {

  import dataAccess._
  import profile.api._

  implicit val executionContext = context.dispatcher

  var participantsMap: Map[UserId, Participant] = Map()
  var messagesList: List[Message] = List()

  @scala.throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    participantsMap = Map(participantsLoaded(chatId).map(participant => (participant.user, participant)): _*)
    messagesList = messagesLoaded(chatId).toList
  }

  override def receive: Receive = {

    case CreateNewMessage(messageSender, createMessageRequest) =>
      val sender = context.sender()
      db.run(participants.isUserParticipant(createMessageRequest.chatId, messageSender)).flatMap{hasAccess =>
        if (hasAccess) {
          db.run(messages.insert(createMessageRequest.chatId, messageSender, createMessageRequest.text))
        } else {
          Future(Forbidden)
        }
      } onComplete {
        case Success(message: Message) => self ! MessageCreated(message, sender)
        case Success(error) => sender ! error
        case Failure(throwable) => sender ! DatabaseError(throwable.getMessage)
      }

    case MessageCreated(message, sender) =>
      messagesList = message :: messagesList
      //TODO send message to all participants and do not send to sender

      sender ! Message


  }
}
