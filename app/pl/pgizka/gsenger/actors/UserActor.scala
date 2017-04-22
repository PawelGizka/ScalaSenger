package pl.pgizka.gsenger.actors

import akka.actor.Actor.Receive
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import pl.pgizka.gsenger.actors.UserActor.{AddedToChat, NewMessage}
import pl.pgizka.gsenger.controllers.message.CreateMessageRequest
import pl.pgizka.gsenger.model._
import pl.pgizka.gsenger.persistance.DatabaseSupport
import pl.pgizka.gsenger.persistance.impl.DAL

object UserActor {

  def props(userId: UserId,
            dataAccess: DAL with DatabaseSupport,
            chatsWithParticipant: Seq[(Chat, Participant)],
            contacts: Seq[(User, Contact)]): Props = Props(classOf[UserActor], userId, dataAccess, chatsWithParticipant, contacts)

  def actorName(userId: UserId): String = s"user-$userId"

  case class NewMessage(message: Message)
  case class AddedToChat(chat: Chat)

  case class CreateNewMessage(createMessageRequest: CreateMessageRequest)
}

class UserActor (userId: UserId,
                 dataAccess: DAL with DatabaseSupport,
                 chatsLoaded: Seq[(Chat, Participant)],
                 contactsLoaded: Seq[(User, Contact)]) extends Actor with ActorLogging {

  import dataAccess._
  import profile.api._
  private var webSockets: Seq[ActorRef] = Seq()

  private var chats: Map[ChatId, Chat] = Map()
  private var contacts: Map[UserId, Contact] = Map()

  implicit val executionContext = context.dispatcher

  @scala.throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    chats = Map(chatsLoaded.map{case (chat, participant) => (chat.id.get, chat)}: _*)
    contacts = Map(contactsLoaded.map{case (user, contact) => (contact.userTo, contact)}: _*)
  }

  override def receive: Receive = {

    case NewMessage(message) =>


    case AddedToChat(chat) =>
      chats = chats.+((chat.id.get, chat))
  }


}
