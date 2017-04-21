package pl.pgizka.gsenger.actors

import akka.actor.Actor.Receive
import akka.actor.{Actor, ActorLogging, ActorRef}
import pl.pgizka.gsenger.actors.UserActor.{AddedToChat, NewMessage}
import pl.pgizka.gsenger.controllers.message.CreateMessageRequest
import pl.pgizka.gsenger.model._
import pl.pgizka.gsenger.persistance.DatabaseSupport
import pl.pgizka.gsenger.persistance.impl.DAL

object UserActor {

  case class NewMessage(message: Message)
  case class AddedToChat(chat: Chat)

  case class CreateNewMessage(sender: UserId, createMessageRequest: CreateMessageRequest)
}

class UserActor (userId: UserId,
                 dataAccess: DAL with DatabaseSupport,
                 chatsLoaded: Map[UserId, Seq[Chat]]) extends Actor with ActorLogging {

  import dataAccess._
  import profile.api._
  private var webSockets: Seq[ActorRef] = Seq()
  private var chats: Map[ChatId, Chat] = Map()

  implicit val executionContext = context.dispatcher


  @scala.throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    chats = Map(chatsLoaded(userId).map(chat => (chat.id.get, chat)): _*)
  }

  override def receive: Receive = {

    case NewMessage(message) =>


    case AddedToChat(chat) =>
      chats = chats.+((chat.id.get, chat))
  }


}
