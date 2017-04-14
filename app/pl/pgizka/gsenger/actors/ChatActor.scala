package pl.pgizka.gsenger.actors

import akka.actor.{Actor, Props}
import akka.pattern._
import akka.actor.Actor.Receive
import pl.pgizka.gsenger.actors.ChatActor.{AccessInformationObtained, ForbiddenResponse, NewMessage, ParticipantsLoaded}
import pl.pgizka.gsenger.controllers.message.CreateMessageRequest
import pl.pgizka.gsenger.model.{ChatId, Message, Participant, UserId}
import pl.pgizka.gsenger.persistance.DatabaseSupport
import pl.pgizka.gsenger.persistance.impl.DAL

import scala.concurrent.ExecutionContext.Implicits.global

object ChatActor {

  def props(chatId: ChatId, dataAccess: DAL with DatabaseSupport): Props = Props(classOf[ChatActor], chatId, dataAccess)
  def actorName(chatId: ChatId): String = s"chat $chatId"

  case class ParticipantsLoaded(participants: Seq[Participant])
  case class AccessInformationObtained(sender: UserId, hasAccess: Boolean, createMessageRequest: CreateMessageRequest)

  case class NewMessage(sender: UserId, createMessageRequest: CreateMessageRequest)

  object ForbiddenResponse
  case class MessageCreatedResponse(message: Message)
}


class ChatActor(chatId: ChatId, dataAccess: DAL with DatabaseSupport) extends Actor{

  import dataAccess._
  import profile.api._

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

    case NewMessage(sender, createMessageRequest) =>
      db.run(participants.isUserParticipant(createMessageRequest.chatId, sender)).map{hasAccess =>
        self forward AccessInformationObtained(sender, hasAccess, createMessageRequest)
      }

    case AccessInformationObtained(senderId, hasAccess, createMessageRequest) =>
      if (hasAccess) {
        db.run(messages.insert(createMessageRequest.chatId, senderId, createMessageRequest.text)) pipeTo sender()
      } else {
        sender() ! ForbiddenResponse
      }

  }
}
