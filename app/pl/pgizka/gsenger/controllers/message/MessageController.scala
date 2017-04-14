package pl.pgizka.gsenger.controllers.message

import pl.pgizka.gsenger.actors.ChatActor
import pl.pgizka.gsenger.actors.ChatActor.NewMessage
import pl.pgizka.gsenger.controllers.CommonController
import pl.pgizka.gsenger.persistance.DatabaseSupport
import pl.pgizka.gsenger.persistance.impl.DAL
import pl.pgizka.gsenger.startup.boot
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Action
import play.api.libs.concurrent.Akka

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class MessageController(override val dataAccess: DAL with DatabaseSupport) extends CommonController(dataAccess) {

  import dataAccess._
  import profile.api._

  def createMessage: Action[JsValue] = Authenticate.async(parse.json) { request =>
    val createMessageRequest = request.body.as[CreateMessageRequest]

    val actorSystem = Akka.system

    val actorSelection = actorSystem.actorSelection("/user/" + ChatActor.actorName(createMessageRequest.chatId))

    actorSelection ! NewMessage(request.user.id.get, createMessageRequest)

    db.run(participants.isUserParticipant(createMessageRequest.chatId, request.user.id.get)).flatMap{hasAccess =>
      if (hasAccess) {
        db.run(messages.insert(createMessageRequest.chatId, request.user.id.get, createMessageRequest.text).transactionally).map{message =>
          Ok(Json.toJson(message))
        } recover databaseError
      } else {
        Future(Forbidden)
      }
    } recover databaseError
  }

}
