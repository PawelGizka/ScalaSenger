package pl.pgizka.gsenger.controllers.message


import pl.pgizka.gsenger.controllers.{CommonController, RestApiErrorResponse}
import pl.pgizka.gsenger.Error
import pl.pgizka.gsenger.errors._
import pl.pgizka.gsenger.actors.ChatActor
import pl.pgizka.gsenger.actors.ChatActor.CreateNewMessage
import pl.pgizka.gsenger.model.Message
import pl.pgizka.gsenger.persistance.DatabaseSupport
import pl.pgizka.gsenger.persistance.impl.DAL

import java.util.concurrent.TimeUnit

import akka.pattern._
import akka.util.Timeout
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

    implicit val timeout = Timeout(5, TimeUnit.MINUTES) //TODO replace with global timeout

    def tryInsert = {
      ChatActor.actorSelection(createMessageRequest.chatId) ? CreateNewMessage(request.user.id.get, createMessageRequest) map {
        case message: Message => Ok(Json.toJson(message))
        case error: Error => BadRequest(Json.toJson(new RestApiErrorResponse(error)))
        case e => BadRequest
      } recover actorAskError
    }

    db.run(chats.find(createMessageRequest.chatId)).flatMap{
      case Some(_) => tryInsert
      case None => Future(NotFound(Json.toJson(new RestApiErrorResponse(CouldNotFindChatError))))
    }


  }

}
