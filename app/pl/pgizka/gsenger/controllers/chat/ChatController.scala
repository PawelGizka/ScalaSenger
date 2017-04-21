package pl.pgizka.gsenger.controllers.chat

import java.time.Instant
import java.util.concurrent.TimeUnit

import akka.actor.{ActorRef, ActorSystem}
import pl.pgizka.gsenger.Utils._
import pl.pgizka.gsenger.actors.ChatManagerActor
import pl.pgizka.gsenger.controllers.{CommonController, RestApiErrorResponse}
import pl.pgizka.gsenger.model.{Chat, User, UserId}
import pl.pgizka.gsenger.persistance.DatabaseSupport
import pl.pgizka.gsenger.persistance.impl.DAL
import pl.pgizka.gsenger.startup.boot
import pl.pgizka.gsenger.Error
import pl.pgizka.gsenger.model.Chat._
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Action
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import play.api.libs.concurrent.Akka

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class ChatController(override val dataAccess: DAL with DatabaseSupport,
                     implicit val actorSystem: ActorSystem,
                     chatManager: ActorRef) extends CommonController(dataAccess) {
  import dataAccess._
  import profile.api._

  def createChat: Action[JsValue] = Authenticate.async(parse.json) { request =>
    val createChatRequest = request.body.as[CreateChatRequest]

    implicit val timeout = Timeout(5, TimeUnit.MINUTES) //TODO replace with global timeout

    chatManager ? ChatManagerActor.CreateNewChat(createChatRequest, request.user) map {
      case chat: Chat => Ok(Json.toJson(chat))
      case error: Error => BadRequest(Json.toJson(new RestApiErrorResponse(error)))
      case e => BadRequest
    } recover actorAskError
  }

  def listAllChatsWithParticipantInfo = Authenticate.async{ request =>
    val chatsInfo= for {
      chatsFound <- db.run(chats.findAllChats(request.user.id.get))
      chatsWithParticipants <- db.run(participants.findAllParticipants(chatsFound))
    } yield chatsWithParticipants.map(ChatInfo(_))

    chatsInfo.map{chatsInfos =>
      Ok(Json.toJson(ListAllChatsWithParticipantInfoResponse(chatsInfos)))
    } recover databaseError
  }

}
