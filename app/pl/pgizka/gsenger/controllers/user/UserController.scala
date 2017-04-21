package pl.pgizka.gsenger.controllers.user

import pl.pgizka.gsenger.actors.WebSocketActor
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

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Future

class UserController(override val dataAccess: DAL with DatabaseSupport, val facebookService: FacebookService) extends CommonController(dataAccess) {
  import dataAccess._
  import profile.api._

  def loginFacebookUser: Action[JsValue] = Action.async(parse.json) { request =>
    val userRegistrationRequest = request.body.as[UserFacebookLoginRequest]

    facebookService.fetchFacebookUser(userRegistrationRequest.facebookToken).flatMap {
      case Right(fbUser) => {

        val dbAction = (for {
          user <- users.insertFacebookUser(fbUser, userRegistrationRequest)
          device <- devices.insertIfNecessary(user, userRegistrationRequest)
          token <- tokens.generateToken(user.id.get)
        } yield (user, token)).transactionally

        db.run(dbAction) map {
          case (user, token) => Ok(toJson(UserLoginRegistrationResponse(user.id.get.value, token.token)))
        } recover databaseError
      }

      case Left(errorInfo) => Future(BadRequest(toJson(new RestApiErrorResponse(FetchFacebookDataError(errorInfo)))))
    }
  }

  def getFriends = Authenticate.async(parse.json) {request =>
    val getFriendsRequest = request.body.as[GetFriendsRequest]

    def friendsFound(dbAction: DBIO[Seq[(User, Contact)]]): Future[Seq[Friend]] = {
      db.run(dbAction).map{users =>
        users.map(tuple => new Friend(tuple._1))
      }
    }

    val list = facebookService.fetchFacebookFriends(request.user.facebookToken.get).flatMap{
      case Right(fbUsers) => friendsFound(contacts.updateContacts(request.user, Some(fbUsers), getFriendsRequest.phoneNumbers))
      case Left(_) => friendsFound(contacts.updateContacts(request.user, None, getFriendsRequest.phoneNumbers))
    }

    list.map {users =>
      Ok(Json.toJson(GetFriendsResponse(users)))
    } recover databaseError
  }

//  def socket = WebSocket.accept[String, String] { request =>
//    ActorFlow.actorRef(out => WebSocketActor.props(out))
//  }

}