package pl.pgizka.gsenger.controllers

import akka.pattern.AskTimeoutException
import pl.pgizka.gsenger.actors.ChatManagerActor
import pl.pgizka.gsenger.errors._
import pl.pgizka.gsenger.model.User
import pl.pgizka.gsenger.persistance.DatabaseSupport
import pl.pgizka.gsenger.persistance.impl.DAL
import play.api.libs.json.Json.toJson
import play.api.mvc._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class CommonController(val dataAccess: DAL with DatabaseSupport) extends Controller{
  import dataAccess._

  class UserRequest[A](val user: User, request: Request[A]) extends WrappedRequest[A](request)

  object Authenticate extends ActionBuilder[UserRequest] {

    def invokeBlock[A](request: Request[A], block: UserRequest[A] => Future[Result]): Future[Result] = {
      if (request.headers.get("accessToken").isEmpty) {
        Future(Forbidden)
      } else {
        val token = request.headers.get("accessToken").get

        db.run(tokens.findUser(token)).flatMap { userOption =>
          userOption.map { user =>
            block(new UserRequest[A](user, request))
          } getOrElse Future(Forbidden)
        } recover {
          case e => Forbidden
        }
      }
    }
  }

  def databaseError: PartialFunction[scala.Throwable, Result] = {
    case e => BadRequest(toJson(new RestApiErrorResponse(DatabaseError(e.getMessage))))
  }

  def actorAskError: PartialFunction[scala.Throwable, Result] = {
    case e: AskTimeoutException => BadRequest(toJson(new RestApiErrorResponse(ActorAskTimeout(e.getMessage))))
    case _ => BadRequest
  }

}
