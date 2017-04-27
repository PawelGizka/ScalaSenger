package pl.pgizka.gsenger.controllers.user

import akka.actor.{ActorNotFound, ActorRef, ActorSystem, Identify}
import pl.pgizka.gsenger.actors.UserActor.{FriendsUpdated, GetFriends}
import pl.pgizka.gsenger.actors.UserManagerActor.UserAdded
import pl.pgizka.gsenger.actors.{UserActor, WebSocketActor}
import pl.pgizka.gsenger.controllers.{CommonController, RestApiErrorResponse}
import pl.pgizka.gsenger.controllers.user._
import pl.pgizka.gsenger.errors._
import pl.pgizka.gsenger.model.{Contact, User}
import pl.pgizka.gsenger.persistance.DatabaseSupport
import pl.pgizka.gsenger.persistance.impl.DAL
import pl.pgizka.gsenger.services.facebook.FacebookService
import play.api.libs.json.Json.toJson
import play.api.libs.json.{JsValue, Json}
import play.api.libs.streams.ActorFlow
import play.api.mvc._
import akka.pattern.{ask, pipe}
import pl.pgizka.gsenger.Error
import pl.pgizka.gsenger.controllers.user.UserController.FriendsUpdated

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Future

object UserController {

  case class FriendsUpdated(friends: Seq[Friend])
}

class UserController(override val dataAccess: DAL with DatabaseSupport,
                     facebookService: FacebookService,
                     implicit val actorSystem: ActorSystem,
                     userManager: ActorRef,
                     chatManager: ActorRef) extends CommonController(dataAccess) {
  import dataAccess._
  import profile.api._

  def loginFacebookUser: Action[JsValue] = LoggAction.async(parse.json) { request =>
    val userRegistrationRequest = request.body.as[UserFacebookLoginRequest]

    facebookService.fetchFacebookUser(userRegistrationRequest.facebookToken).flatMap {
      case Right(fbUser) => {

        val dbAction = (for {
          user <- users.insertFacebookUser(fbUser, userRegistrationRequest)
          device <- devices.insertIfNecessary(user, userRegistrationRequest)
          token <- tokens.generateToken(user.id.get)
        } yield (user, token)).transactionally

        db.run(dbAction) map {
          case (user, token) =>
            userManager ! UserAdded(user)
            Ok(toJson(UserLoginRegistrationResponse(user.id.get.value, token.token)))
        } recover databaseError
      }

      case Left(errorInfo) => Future(BadRequest(toJson(new RestApiErrorResponse(FetchFacebookDataError(errorInfo)))))
    }
  }

  def getFriends = AuthenticateWithLogAction.async(parse.json) { request =>
    val getFriendsRequest = request.body.as[GetFriendsRequest]

    UserActor.actorSelection(request.user.id.get, actorSystem) ? GetFriends(getFriendsRequest) map {
      case FriendsUpdated(friends) => Ok(Json.toJson(GetFriendsResponse(friends)))
      case error: Error => BadRequest(Json.toJson(new RestApiErrorResponse(error)))
      case e => BadRequest
    } recover actorAskError

  }


  def socket = WebSocket.acceptOrResult[String, String] { request =>
    validateRequest(request).flatMap {userOption =>
      if (userOption.isDefined) {
        UserActor.actorSelection(userOption.get.id.get, actorSystem).resolveOne() map {userActorRef =>
          Right(ActorFlow.actorRef(out => WebSocketActor.props(out, userOption.get.id.get, userActorRef, chatManager)))
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