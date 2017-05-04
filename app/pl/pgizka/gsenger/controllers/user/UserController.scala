package pl.pgizka.gsenger.controllers.user

import pl.pgizka.gsenger.actors.UserActor.GetContacts
import pl.pgizka.gsenger.actors.UserManagerActor.UserAdded
import pl.pgizka.gsenger.actors.{ActorErrorResponse, ActorResponse, UserActor, WebSocketActor}
import pl.pgizka.gsenger.controllers.{CommonController, RestApiErrorResponse}
import pl.pgizka.gsenger.errors._
import pl.pgizka.gsenger.persistance.DatabaseSupport
import pl.pgizka.gsenger.persistance.impl.DAL
import pl.pgizka.gsenger.services.facebook.FacebookService
import pl.pgizka.gsenger.Error
import pl.pgizka.gsenger.startup.Implicits.timeout

import scala.concurrent.Future
import akka.actor.{ActorNotFound, ActorRef, ActorSystem}
import akka.pattern.ask
import akka.stream.Materializer
import pl.pgizka.gsenger.dtos.users._
import play.api.mvc._
import play.api.libs.json.Json.toJson
import play.api.libs.json.{JsValue, Json}
import play.api.libs.streams.ActorFlow
import play.api.libs.concurrent.Execution.Implicits.defaultContext

class UserController(override val dataAccess: DAL with DatabaseSupport,
                     facebookService: FacebookService,
                     implicit val actorSystem: ActorSystem,
                     implicit val materializer: Materializer,
                     userManager: ActorRef,
                     chatManager: ActorRef) extends CommonController(dataAccess) {
  import dataAccess._
  import profile.api._

  def loginFacebookUser: Action[JsValue] = LoggAction.async(parse.json) { request =>
    val userRegistrationRequest = request.body.as[UserFacebookLoginRequestDto]

    facebookService.fetchFacebookUser(userRegistrationRequest.facebookToken).flatMap {
      case Right(fbUser) =>

        val dbAction = (for {
          user <- users.insertFacebookUser(fbUser, userRegistrationRequest)
          device <- devices.insertIfNecessary(user, userRegistrationRequest)
          token <- tokens.generateToken(user.id.get)
        } yield (user, token)).transactionally

        db.run(dbAction) map {
          case (user, token) =>
            userManager ! UserAdded(user)
            Ok(toJson(UserLoginRegistrationResponseDto(user.id.get.value, token.token)))
        } recover databaseError

      case Left(errorInfo) => Future(BadRequest(toJson(new RestApiErrorResponse(FetchFacebookDataError(errorInfo)))))
    }
  }

  def getContacts: Action[JsValue] = AuthenticateWithLogAction.async(parse.json) { request =>
    val getFriendsRequest = request.body.as[GetContactsRequestDto]

    UserActor.actorSelection(request.user.id.get) ? GetContacts(getFriendsRequest) map {
      case UserActor.GetContactsResponse(contacts: Seq[ContactDto], _) => Ok(Json.toJson(new GetContactsResponseDto(contacts)))
      case ActorErrorResponse(error: Error, _) => BadRequest(Json.toJson(new RestApiErrorResponse(error)))
      case e => BadRequest
    } recover actorAskError
  }

  def socket: WebSocket = WebSocket.acceptOrResult[String, String] { request =>
    validateRequest(request).flatMap {userOption =>
      if (userOption.isDefined) {
        UserActor.actorSelection(userOption.get.id.get).resolveOne() map {userActorRef =>
          Right(ActorFlow.actorRef(out => WebSocketActor.props(out, userOption.get.id.get, userActorRef, dataAccess)))
        } recover {
          case e: ActorNotFound => Left(Forbidden)
        }
      } else {
        Future(Left(Forbidden))
      }
    } recover {
      case e => Left(Forbidden)
    }
  }

}