package pl.pgizka.gsenger.controllers.message

import pl.pgizka.gsenger.controllers.{CommonController, ErrorResponse}
import pl.pgizka.gsenger.controllers.errors._
import pl.pgizka.gsenger.controllers.ErrorResponse._
import pl.pgizka.gsenger.persistance.DatabaseSupport
import pl.pgizka.gsenger.persistance.impl.DAL
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Action

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class MessageController(override val dataAccess: DAL with DatabaseSupport) extends CommonController(dataAccess) {

  import dataAccess._
  import profile.api._

  def createMessage: Action[JsValue] = Authenticate.async(parse.json) { request =>
    val createMessageRequest = request.body.as[CreateMessageRequest]

    def tryInsert = {
      db.run(participants.isUserParticipant(createMessageRequest.chatId, request.user.id.get)).flatMap { hasAccess =>
        if (hasAccess) {
          db.run(messages.insert(createMessageRequest.chatId, request.user.id.get, createMessageRequest.text).transactionally).map { message =>
            Ok(Json.toJson(message))
          } recover databaseError
        } else {
          Future(Forbidden)
        }
      } recover databaseError
    }

    db.run(chats.find(createMessageRequest.chatId)).flatMap{
      case Some(_) => tryInsert
      case None => Future(NotFound(Json.toJson(new ErrorResponse(CouldNotFindChatError))))
    }


  }

}
