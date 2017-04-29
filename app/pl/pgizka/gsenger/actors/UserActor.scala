package pl.pgizka.gsenger.actors

import pl.pgizka.gsenger.actors.UserActor._
import pl.pgizka.gsenger.controllers.message.CreateMessageRequest
import pl.pgizka.gsenger.controllers.user.UserController.FriendsUpdated
import pl.pgizka.gsenger.controllers.user.{Friend, GetFriendsRequest}
import pl.pgizka.gsenger.errors.DatabaseError
import pl.pgizka.gsenger.model._
import pl.pgizka.gsenger.persistance.DatabaseSupport
import pl.pgizka.gsenger.persistance.impl.DAL
import pl.pgizka.gsenger.services.facebook.FacebookService

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSelection, ActorSystem, Props}

import scala.concurrent.Future
import scala.util.{Failure, Success}

object UserActor {

  def props(user: User,
            dataAccess: DAL with DatabaseSupport,
            chatsWithParticipant: Seq[(Chat, Participant)],
            contacts: Seq[(User, Contact)],
            facebookService: FacebookService): Props = Props(classOf[UserActor], user, dataAccess, chatsWithParticipant, contacts, facebookService)

  def actorName(userId: UserId): String = s"user-$userId"

  def actorSelection(userId: UserId, actorSystem: ActorSystem): ActorSelection = actorSystem.actorSelection("user/users/" + actorName(userId))

  case class NewMessage(message: Message)
  case class AddedToChat(chat: Chat)
  case class GetFriends(getFriendsRequest: GetFriendsRequest)
  case class CreateNewMessage(createMessageRequest: CreateMessageRequest)

  case class NewWebSocketConnection(webSocketActor: ActorRef)
  case class WebSocketConnectionClosed(webSocketActor: ActorRef)

  private case class ContactsUpdated(contacts: Seq[(User, Contact)])
}

class UserActor (user: User,
                 dataAccess: DAL with DatabaseSupport,
                 chatsLoaded: Seq[(Chat, Participant)],
                 contactsLoaded: Seq[(User, Contact)],
                 facebookService: FacebookService) extends Actor with ActorLogging {

  import dataAccess.db
  import dataAccess.profile.api._
  private var webSockets: List[ActorRef] = List()

  private var chats: Map[ChatId, Chat] = Map()
  private var contacts: Map[UserId, Contact] = Map()

  implicit val executionContext = context.dispatcher

  @scala.throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    chats = Map(chatsLoaded.map{case (chat, participant) => (chat.id.get, chat)}: _*)
    contacts = Map(contactsLoaded.map{case (user, contact) => (contact.userTo, contact)}: _*)
  }

  override def receive: Receive = {

    case GetFriends(getFriendsRequest) =>
      val sender = context.sender()

      def friendsFound(dbAction: DBIO[Seq[(User, Contact)]]): Future[(Seq[(User, Contact)], Seq[Friend])] = {
        db.run(dbAction).map{contacts =>
          (contacts, contacts.map(tuple => new Friend(tuple._1)))
        }
      }

      facebookService.fetchFacebookFriends(user.facebookToken.get).flatMap{
        case Right(fbUsers) => friendsFound(dataAccess.contacts.updateContacts(user, Some(fbUsers), getFriendsRequest.phoneNumbers))
        case Left(_) => friendsFound(dataAccess.contacts.updateContacts(user, None, getFriendsRequest.phoneNumbers))
      } onComplete {
        case Success((contacts: Seq[(User, Contact)], friends: Seq[Friend])) =>
          self ! ContactsUpdated(contacts)
          sender ! FriendsUpdated(friends)
        case Failure(throwable) => sender ! DatabaseError(throwable.getMessage)
      }

    case ContactsUpdated(contactsUpdated) =>
      contacts = Map(contactsUpdated.map{case (userContact, contact) => (userContact.id.get, contact)}: _*)


    case NewMessage(message) =>


    case AddedToChat(chat) =>
      chats = chats.+((chat.id.get, chat))

    case NewWebSocketConnection(webSocketActor) =>
      webSockets = webSocketActor :: webSockets

    case WebSocketConnectionClosed(webSocketActor) =>
      webSockets = webSockets.filter(actorRef => actorRef != webSocketActor)
  }


}
