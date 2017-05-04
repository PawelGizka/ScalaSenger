package pl.pgizka.gsenger.actors

import pl.pgizka.gsenger.model._
import pl.pgizka.gsenger.Error
import pl.pgizka.gsenger.actors.WebSocketRequest._
import pl.pgizka.gsenger.actors.WebSocketActor.{AddedToChat, NewMessage}
import pl.pgizka.gsenger.errors.DatabaseError
import pl.pgizka.gsenger.persistance.DatabaseSupport
import pl.pgizka.gsenger.persistance.impl.DAL
import play.api.libs.json.{JsValue, Json}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.{ask, pipe}
import pl.pgizka.gsenger.actors.ActorsUtils.databaseError
import pl.pgizka.gsenger.dtos.chats.{ChatDto, CreateChatRequestDto}
import pl.pgizka.gsenger.dtos.messages.{CreateMessageRequestDto, MessageDto}
import pl.pgizka.gsenger.dtos.users.{GetContactsRequestDto, GetContactsResponseDto}

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
                      chatManagerActor: ActorRef,
                      dataAccess: DAL with DatabaseSupport) extends Actor with ActorLogging {

  import dataAccess._
  import profile.api._

  @scala.throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    userActor ! UserActor.NewWebSocketConnection(self)
  }

  @scala.throws[Exception](classOf[Exception])
  override def postStop(): Unit = {
    userActor ! UserActor.WebSocketConnectionClosed(self)
  }

  override def receive: Receive = {

    case NewMessage(message) =>
      out ! WebSocketPush("newMessage", Json.toJson(new MessageDto(message)))

    case AddedToChat(chat, participants) =>
      out ! WebSocketPush("addedToChat", Json.toJson(new ChatDto(chat, participants)))

    case ActorErrorResponse(error: Error, requestContext) =>
      out ! new WebSocketErrorResponse(requestContext, error)

    case ChatActor.CreateNewMessageResponse(message, requestContext)=>
      out ! new WebSocketResponse(requestContext, Json.toJson(new MessageDto(message)))

    case ChatManagerActor.CreateNewChatResponse(chat, participants, requestContext) =>
      out ! new WebSocketResponse(requestContext, Json.toJson(new ChatDto(chat, participants)))

    case UserActor.GetContactsResponse(contactsDtos, requestContext) =>
      out ! new WebSocketResponse(requestContext, Json.toJson(new GetContactsResponseDto(contactsDtos)))

    case js: JsValue =>
      val request = js.as[WebSocketRequest]
      val method = request.method
      val id = request.id
      val content = request.content

      method match {
        case "createNewMessage" =>
          val createMessageRequest = content.as[CreateMessageRequestDto]
          ChatActor.actorSelection(createMessageRequest.chatId)(context.system) forward
            ChatActor.CreateNewMessage(userId, createMessageRequest, new RequestContext(request))

        case "createNewChat" =>
          chatManagerActor ! ChatManagerActor.CreateNewChat(content.as[CreateChatRequestDto], userId, new RequestContext(request))

        case "getFriends" =>
          userActor ! UserActor.GetContacts(content.as[GetContactsRequestDto], new RequestContext(request))

        case "listChats" =>
          db.run(chats.findAllChatsWithParticipants(userId)).map{chatsInfos =>
            WebSocketResponse(Some(method), id, Json.toJson(chatsInfos))
          } recover databaseError(request) pipeTo out
      }

  }

}
