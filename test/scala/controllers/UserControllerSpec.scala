package scala.controllers


import pl.pgizka.gsenger.actors.{ChatManagerActor, UserManagerActor}
import pl.pgizka.gsenger.controllers.user.UserController
import pl.pgizka.gsenger.dtos.users.{UserFacebookLoginRequestDto, UserLoginRegistrationResponseDto}
import pl.pgizka.gsenger.services.facebook.{FacebookService, FbUser, realFacebookService}
import pl.pgizka.gsenger.startup.InitialData

import akka.actor.ActorRef
import akka.stream.ActorMaterializer

import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._

import org.mockito.Mockito._
import org.specs2.mock.mockito.MockitoMatchers._

import scala.Utils._
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global

class UserControllerSpec extends ControllerSpec {

  val facebookService = mock[FacebookService]
  var userController: UserController = _

  var chatManager: ActorRef = _
  var userManager: ActorRef = _

  override def onBefore(initialData: InitialData): Unit = {
    chatManager = system.actorOf(ChatManagerActor.props(this, initialData), "chatManager")
    userManager = system.actorOf(UserManagerActor.props(this, initialData, realFacebookService), "userManager")

    userController = new UserController(this, facebookService, system, ActorMaterializer()(system), userManager, chatManager)
  }

  override def onAfter(): Unit = {
    system.stop(chatManager)
    system.stop(userManager)
  }

  val loginRequest = UserFacebookLoginRequestDto(123, "device id 2", "gcm token 2", "facebook token 2")

  "loginFacebookUser" should {
    "return 200, new user id = 2 and access token when inserting new user" in {
      val fbUser = FbUser("id", Some("email"), "firstName", "name", Some("gender"))
      when(facebookService.fetchFacebookUser(any[String])(any[ExecutionContext])) thenReturn Future(Right(fbUser))

      val request = FakeRequest().withBody(Json.toJson(loginRequest))
      val response = userController.loginFacebookUser.apply(request)

      status(response) must equal(200)

      val responseBody = contentAs[UserLoginRegistrationResponseDto](response)
      responseBody.userId must equal(6)
    }

    "return 400 when there is problem with fetching facebook user" in {
      when(facebookService.fetchFacebookUser(any[String])(any[ExecutionContext])) thenReturn Future(Left("problem with access token"))

      val request = FakeRequest().withBody(Json.toJson(loginRequest))
      val response = userController.loginFacebookUser.apply(request)

      status(response) must equal(400)
    }
  }

}
