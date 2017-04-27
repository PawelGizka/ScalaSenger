package pl.pgizka.gsenger.controllers

import java.util.{Calendar, Date}

import akka.pattern.AskTimeoutException
import pl.pgizka.gsenger.actors.ChatManagerActor
import pl.pgizka.gsenger.errors._
import pl.pgizka.gsenger.model.User
import pl.pgizka.gsenger.persistance.DatabaseSupport
import pl.pgizka.gsenger.persistance.impl.DAL
import play.api.Logger
import play.api.http.MediaType
import play.api.libs.json.Json.toJson
import play.api.mvc._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class CommonController(val dataAccess: DAL with DatabaseSupport) extends Controller{
  import dataAccess._

  val logger: Logger = Logger("http")

  case class UserRequest[A](val user: User, request: Request[A]) extends WrappedRequest[A](request)

  val AuthenticateWithLogAction: ActionBuilder[UserRequest] = LoggAction andThen AuthenticateAction

  object AuthenticateAction extends ActionBuilder[UserRequest] {

    def invokeBlock[A](request: Request[A], block: UserRequest[A] => Future[Result]): Future[Result] = {
      validateRequest(request).flatMap{userOption =>
        if (userOption.isDefined) {
          block(new UserRequest[A](userOption.get, request))
        } else {
          Future(Forbidden)
        }
      } recover {
        case e => Forbidden
      }
    }

  }

  object LoggAction extends ActionBuilder[Request] {
    def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]): Future[Result] = {

      val startTime = System.currentTimeMillis()

      val userInfo: String = request match {
        case UserRequest(user, _) => s"user=${user.id}"
        case e => "user=unknown"
      }

      val info = s"${request.method} ${request.uri} $userInfo ${new Date().toString}"

      logger.info(s"Request -> $info")
      logger.debug(s"Headers: ${request.headers}")

      val result = block(request)

      result.map{result =>
        val time = s" ${(System.currentTimeMillis() - startTime).asInstanceOf[Double] / 1000} seconds"
        val resultInfo = s"Result <- $info took: $time status: ${result.header.status}"

        if (result.header.status >= 200 && result.header.status <= 299) {
          logger.info(resultInfo)
        } else {
          if (request.contentType.contains("application/json")) {
            logger.warn(s" $resultInfo Response Body: ${result.body}")
          } else {
            logger.warn(resultInfo)
          }
        }

      }
      result
    }
  }

  def validateRequest(request: RequestHeader): Future[Option[User]] = {
    if (request.headers.get("accessToken").isEmpty) {
      Future(None)
    } else {
      val token = request.headers.get("accessToken").get
      db.run(tokens.findUser(token))
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
