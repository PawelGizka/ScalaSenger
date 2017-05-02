package pl.pgizka.gsenger.controllers.chat

import pl.pgizka.gsenger.actors.{ActorResponse, ChatManagerActor}
import pl.pgizka.gsenger.controllers.{CommonController, RestApiErrorResponse}
import pl.pgizka.gsenger.model.{Chat, Participant}
import pl.pgizka.gsenger.persistance.DatabaseSupport
import pl.pgizka.gsenger.persistance.impl.DAL
import pl.pgizka.gsenger.Error
import pl.pgizka.gsenger.model.Chat._
import pl.pgizka.gsenger.startup.Implicits.timeout
import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Action


class ChatController(override val dataAccess: DAL with DatabaseSupport,
                     implicit val actorSystem: ActorSystem,
                     chatManager: ActorRef) extends CommonController(dataAccess) {
  import dataAccess._
  import profile.api._

  def createChat: Action[JsValue] = AuthenticateWithLogAction.async(parse.json) { request =>
    val createChatRequest = request.body.as[CreateChatRequest]

    chatManager ? ChatManagerActor.CreateNewChat(createChatRequest, request.user) map {
      case ActorResponse((chat: Chat, participants: Seq[Participant]), _) => Ok(Json.toJson(chat))
      case ActorResponse(error: Error, _) => BadRequest(Json.toJson(new RestApiErrorResponse(error)))
      case e => BadRequest
    } recover actorAskError
  }

  def listAllChatsWithParticipantInfo = AuthenticateWithLogAction.async{ request =>
    val chatsInfos = db.run(chats.findAllChatsWithParticipants(request.user.id.get))

    chatsInfos.map{chatsInfos =>
      Ok(Json.toJson(ListAllChatsWithParticipantInfoResponse(chatsInfos)))
    } recover databaseError
  }

}
