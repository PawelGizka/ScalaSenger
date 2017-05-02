package pl.pgizka.gsenger.actors

import pl.pgizka.gsenger.actors.UserActor._
import pl.pgizka.gsenger.controllers.message.CreateMessageRequest
import pl.pgizka.gsenger.controllers.user.{Friend, GetFriendsRequest}
import pl.pgizka.gsenger.errors.{DatabaseError, Forbidden}
import pl.pgizka.gsenger.model._
import pl.pgizka.gsenger.persistance.DatabaseSupport
import pl.pgizka.gsenger.persistance.impl.DAL
import pl.pgizka.gsenger.services.facebook.FacebookService
import akka.actor.{Actor, ActorLogging, ActorRef, ActorSelection, ActorSystem, Props}
import pl.pgizka.gsenger.controllers.chat.CreateChatRequest

import scala.concurrent.Future

object UserActor {

  def props(user: User,
            dataAccess: DAL with DatabaseSupport,
            chatsWithParticipant: Seq[(Chat, Participant)],
            contacts: Seq[(User, Contact)],
            facebookService: FacebookService,
            chatManager: ActorRef): Props = Props(classOf[UserActor], user, dataAccess, chatsWithParticipant, contacts, facebookService, chatManager)

  def actorName(userId: UserId): String = s"user-$userId"

  def actorSelection(userId: UserId)(implicit actorSystem: ActorSystem): ActorSelection = actorSystem.actorSelection("user/userManager/" + actorName(userId))

  case class NewMessage(message: Message)
  case class AddedToChat(chat: Chat, participants: Seq[Participant])

  case class GetFriends(getFriendsRequest: GetFriendsRequest, requestContext: RequestContext = RequestContext())
  case class CreateNewMessage(createMessageRequest: CreateMessageRequest, requestContext: RequestContext = RequestContext())
  case class CreateNewChat(createChatRequest: CreateChatRequest, requestContext: RequestContext = RequestContext())

  //responses
  case class GetFriendsResponse(friends: Seq[Friend], override val requestContext: RequestContext) extends ActorResponse

  case class NewWebSocketConnection(webSocketActor: ActorRef)
  case class WebSocketConnectionClosed(webSocketActor: ActorRef)

  private case class ContactsUpdated(contacts: Seq[(User, Contact)])
}

class UserActor (user: User,
                 dataAccess: DAL with DatabaseSupport,
                 chatsLoaded: Seq[(Chat, Participant)],
                 contactsLoaded: Seq[(User, Contact)],
                 facebookService: FacebookService,
                 chatManager: ActorRef) extends Actor with ActorLogging {

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

    case GetFriends(getFriendsRequest, requestContext) =>
      val sender = context.sender()

      case class Result(contactsUpdated: Seq[(User, Contact)], friendsUpdated: Seq[Friend])

      def friendsFound(dbAction: DBIO[Seq[(User, Contact)]]): Future[Result] = {
        db.run(dbAction).map{contacts =>
          Result(contacts, contacts.map(tuple => new Friend(tuple._1)))
        }
      }

      facebookService.fetchFacebookFriends(user.facebookToken.get).flatMap{
        case Right(fbUsers) => friendsFound(dataAccess.contacts.updateContacts(user, Some(fbUsers), getFriendsRequest.phoneNumbers))
        case Left(_) => friendsFound(dataAccess.contacts.updateContacts(user, None, getFriendsRequest.phoneNumbers))
      } onComplete ActorsUtils.handleDbComplete(sender, requestContext) {
        case Result(contactsUpdated, friendsUpdated) =>
          self ! ContactsUpdated(contactsUpdated)
          sender ! GetFriendsResponse(friendsUpdated, requestContext)
      }

    case ContactsUpdated(contactsUpdated) =>
      contacts = Map(contactsUpdated.map{case (userContact, contact) => (userContact.id.get, contact)}: _*)

    case CreateNewMessage(createMessageRequest, requestContext) =>
      val hasAccess = chats.get(createMessageRequest.chatId).isDefined
      if (hasAccess) {
        ChatActor.actorSelection(createMessageRequest.chatId)(context.system) forward
          ChatActor.CreateNewMessage(user.id.get, createMessageRequest, requestContext)
      } else {
        sender() ! ActorErrorResponse(Forbidden, requestContext)
      }

    case CreateNewChat(createChatRequest, requestContext) =>
      chatManager forward ChatManagerActor.CreateNewChat(createChatRequest, user, requestContext)

    case NewMessage(message) =>
      webSockets.foreach(webSocket => webSocket ! WebSocketActor.NewMessage(message))

    case AddedToChat(chat, participants) =>
      println("added to chat")
      chats = chats.+((chat.id.get, chat))
      webSockets.foreach{webSocket =>
        webSocket ! WebSocketActor.AddedToChat(chat, participants)
      }

    case NewWebSocketConnection(webSocketActor) =>
      webSockets = webSocketActor :: webSockets

    case WebSocketConnectionClosed(webSocketActor) =>
      webSockets = webSockets.filter(actorRef => actorRef != webSocketActor)
  }


}
