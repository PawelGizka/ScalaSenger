package pl.pgizka.gsenger.actors

import pl.pgizka.gsenger.controllers.message.CreateMessageRequest
import pl.pgizka.gsenger.model._
import pl.pgizka.gsenger.Error
import pl.pgizka.gsenger.actors.WebSocketRequest._
import pl.pgizka.gsenger.actors.WebSocketActor.{AddedToChat, NewMessage}
import pl.pgizka.gsenger.controllers.chat.{ChatInfo, CreateChatRequest}
import pl.pgizka.gsenger.controllers.user.{Friend, GetFriendsRequest, GetFriendsResponse}
import pl.pgizka.gsenger.errors.DatabaseError
import pl.pgizka.gsenger.persistance.DatabaseSupport
import pl.pgizka.gsenger.persistance.impl.DAL
import play.api.libs.json.{JsValue, Json}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.{ask, pipe}
import pl.pgizka.gsenger.actors.ActorsUtils.databaseError

object WebSocketActor {

  def props(out: ActorRef,
            userId: UserId,
            userActor: ActorRef,
            dataAccess: DAL with DatabaseSupport) =

    Props(classOf[WebSocketActor], out, userId, userActor, dataAccess)

  case class NewMessage(message: Message)
  case class AddedToChat(chat: Chat, participants: Seq[Participant])
}

class WebSocketActor (out: ActorRef,
                      userId: UserId,
                      userActor: ActorRef,
                      dataAccess: DAL with DatabaseSupport) extends Actor with ActorLogging {

  import dataAccess._
  import profile.api._

  @scala.throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    userActor ! UserActor.NewWebSocketConnection(self)
  }

  override def receive: Receive = {

    case NewMessage(message) =>
      out ! WebSocketPush("newMessage", Json.toJson(message))

    case AddedToChat(chat, participants) =>
      out ! WebSocketPush("addedToChat", Json.toJson(new ChatInfo(chat, participants)))

    case ActorResponse(error: Error, requestContext) =>
      out ! new WebSocketErrorResponse(requestContext, error)

    case ActorResponse(message: Message, requestContext) =>
      out ! new WebSocketResponse(requestContext, Json.toJson(message))

    case ActorResponse((chat: Chat, participants: Seq[Participant]), requestContext) =>
      out ! new WebSocketResponse(requestContext, Json.toJson(new ChatInfo(chat, participants)))

    case ActorResponse(friends: Seq[Friend], requestContext) =>
      out ! new WebSocketResponse(requestContext, Json.toJson(new GetFriendsResponse(friends)))

    case js: JsValue =>
      val request = js.as[WebSocketRequest]
      val method = request.method
      val id = request.id
      val content = request.content

      method match {
        case "createNewMessage" =>
          userActor ! UserActor.CreateNewMessage(content.as[CreateMessageRequest], new RequestContext(request))

        case "createNewChat" =>
          userActor ! UserActor.CreateNewChat(content.as[CreateChatRequest], new RequestContext(request))

        case "getFriends" =>
          userActor ! UserActor.GetFriends(content.as[GetFriendsRequest], new RequestContext(request))

        case "listChats" =>
          db.run(chats.findAllChatsWithParticipants(userId)).map{chatsInfos =>
            WebSocketResponse(Some(method), id, Json.toJson(chatsInfos))
          } recover databaseError(request) pipeTo out
      }

  }

  @scala.throws[Exception](classOf[Exception])
  override def postStop(): Unit = {
    userActor ! UserActor.WebSocketConnectionClosed(self)
  }
}
