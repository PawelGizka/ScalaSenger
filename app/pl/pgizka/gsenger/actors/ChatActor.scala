package pl.pgizka.gsenger.actors

import akka.actor.{Actor, Props}
import akka.pattern._
import akka.actor.Actor.Receive
import pl.pgizka.gsenger.actors.ChatActor.{AccessInformationObtained, CreateNewMessage, ParticipantsLoaded}
import pl.pgizka.gsenger.controllers.message.CreateMessageRequest
import pl.pgizka.gsenger.errors.Forbidden
import pl.pgizka.gsenger.model.{ChatId, Message, Participant, UserId}
import pl.pgizka.gsenger.persistance.DatabaseSupport
import pl.pgizka.gsenger.persistance.impl.DAL

object ChatActor {

  def props(chatId: ChatId, dataAccess: DAL with DatabaseSupport): Props = Props(classOf[ChatActor], chatId, dataAccess)
  def actorName(chatId: ChatId): String = s"chat $chatId"

  case class ParticipantsLoaded(participants: Seq[Participant])
  case class AccessInformationObtained(sender: UserId, hasAccess: Boolean, createMessageRequest: CreateMessageRequest)

  case class CreateNewMessage(sender: UserId, createMessageRequest: CreateMessageRequest)
}


class ChatActor(chatId: ChatId, dataAccess: DAL with DatabaseSupport) extends Actor{

  import dataAccess._
  import profile.api._

  implicit val executionContext = context.dispatcher

  var participantsList: List[Participant] = List()
  var messagesList: List[Message] = List()

  @scala.throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    println("pre start")
    db.run(participants.findAllParticipants(chatId)).map(participants => self forward ParticipantsLoaded(participants))
  }

  override def receive: Receive = {
    case ParticipantsLoaded(participants) =>
      println("participants size " + participants.size)
      participantsList = participants.toList

    case CreateNewMessage(sender, createMessageRequest) =>
      db.run(participants.isUserParticipant(createMessageRequest.chatId, sender)).map{hasAccess =>
        AccessInformationObtained(sender, hasAccess, createMessageRequest)
      }  pipeTo self

    case AccessInformationObtained(senderId, hasAccess, createMessageRequest) =>
      if (hasAccess) {
        db.run(messages.insert(createMessageRequest.chatId, senderId, createMessageRequest.text)) pipeTo sender()
      } else {
        sender() ! Forbidden
      }

  }
}
