package pl.pgizka.gsenger.controllers.chat

import pl.pgizka.gsenger.actors.{ActorErrorResponse, ChatManagerActor}
import pl.pgizka.gsenger.controllers.{CommonController, RestApiErrorResponse}
import pl.pgizka.gsenger.persistance.DatabaseSupport
import pl.pgizka.gsenger.persistance.impl.DAL
import pl.pgizka.gsenger.Error
import pl.pgizka.gsenger.model.Chat._
import pl.pgizka.gsenger.startup.Implicits.akkAskTimeout
import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask
import pl.pgizka.gsenger.dtos.chats.{CreateChatRequestDto, GetAllChatsWithParticipantInfoDto}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Action


class ChatController(override val dataAccess: DAL with DatabaseSupport,
                     implicit val actorSystem: ActorSystem,
                     chatManager: ActorRef) extends CommonController(dataAccess) {
  import dataAccess._
  import profile.api._

  def createChat: Action[JsValue] = AuthenticateWithLogAction.async(parse.json) { request =>
    val createChatRequest = request.body.as[CreateChatRequestDto]

    chatManager ? ChatManagerActor.CreateNewChat(createChatRequest, request.user.id.get) map {
      case ChatManagerActor.CreateNewChatResponse(chat, participants, _) => Ok(Json.toJson(chat))
      case ActorErrorResponse(error: Error, _) => BadRequest(Json.toJson(new RestApiErrorResponse(error)))
      case e => BadRequest
    } recover actorAskError
  }

  def getAllChatsWithParticipantInfo = AuthenticateWithLogAction.async{ request =>
    val chatsInfos = db.run(chats.findAllChatsWithParticipants(request.user.id.get))

    chatsInfos.map{chatsInfos =>
      Ok(Json.toJson(GetAllChatsWithParticipantInfoDto(chatsInfos)))
    } recover databaseError
  }

}
