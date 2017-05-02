package pl.pgizka.gsenger.controllers.message


import pl.pgizka.gsenger.controllers.{CommonController, RestApiErrorResponse}
import pl.pgizka.gsenger.Error
import pl.pgizka.gsenger.errors._
import pl.pgizka.gsenger.actors.{ActorErrorResponse, ActorResponse, ChatActor}
import pl.pgizka.gsenger.actors.ChatActor.CreateNewMessage
import pl.pgizka.gsenger.model.Message
import pl.pgizka.gsenger.persistance.DatabaseSupport
import pl.pgizka.gsenger.persistance.impl.DAL
import pl.pgizka.gsenger.startup.Implicits.timeout
import akka.pattern._
import akka.actor.ActorSystem

import scala.concurrent.Future
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Action
import play.api.libs.concurrent.Execution.Implicits.defaultContext


class MessageController(override val dataAccess: DAL with DatabaseSupport, implicit val actorSystem: ActorSystem) extends CommonController(dataAccess) {

  import dataAccess._
  import profile.api._

  def createMessage: Action[JsValue] = AuthenticateWithLogAction.async(parse.json) { request =>
    val createMessageRequest = request.body.as[CreateMessageRequest]

    def tryInsert = {
      ChatActor.actorSelection(createMessageRequest.chatId) ? CreateNewMessage(request.user.id.get, createMessageRequest) map {
        case ChatActor.CreateNewMessageResponse(message: Message, _) => Ok(Json.toJson(message))
        case ActorErrorResponse(error: Error, _) => BadRequest(Json.toJson(new RestApiErrorResponse(error)))
        case e => BadRequest
      } recover actorAskError
    }

    db.run(chats.find(createMessageRequest.chatId)).flatMap{
      case Some(_) => tryInsert
      case None => Future(NotFound(Json.toJson(new RestApiErrorResponse(CouldNotFindChatError))))
    }


  }

}
