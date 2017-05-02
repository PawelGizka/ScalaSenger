package scala.controllers

import akka.actor.{ActorRef, ActorSystem}
import akka.stream.ActorMaterializer
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import org.mockito.Mockito._
import org.specs2.mock.mockito.MockitoMatchers._
import pl.pgizka.gsenger.actors.{ChatManagerActor, UserManagerActor}
import pl.pgizka.gsenger.controllers.user.{UserController, UserFacebookLoginRequest, UserLoginRegistrationResponse}
import pl.pgizka.gsenger.services.facebook.{FacebookService, FbUser, realFacebookService}
import pl.pgizka.gsenger.startup.InitialData

import scala.Utils._
import scala.concurrent.{Await, Awaitable, ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

class UserControllerSpec extends ControllerSpecWithDefaultScenario {
  implicit lazy val system = ActorSystem()

  import profile.api._
  import pl.pgizka.gsenger.startup.DefaultScenario._

  val facebookService = mock[FacebookService]
  var userController: UserController = _

  var chatManager: ActorRef = _
  var userManager: ActorRef = _

  before {
    db.run(createDefaultScenarioAction).futureValue

    val initialData = await(InitialData.load(this))

    chatManager = system.actorOf(ChatManagerActor.props(this, initialData), "chatManager")
    userManager = system.actorOf(UserManagerActor.props(this, initialData, realFacebookService, chatManager), "userManager")

    userController = new UserController(this, facebookService, system, ActorMaterializer()(system), userManager, chatManager)
  }

  def await[T](awaitable: Awaitable[T]): T = {
    Await.result(awaitable, Duration.Inf)
  }

  after {
    db.run(schema.drop).futureValue
    system.stop(chatManager)
    system.stop(userManager)
  }

  val loginRequest = UserFacebookLoginRequest(123, "device id 2", "gcm token 2", "facebook token 2")

  "loginFacebookUser" should {
    "return 200, new user id = 2 and access token when inserting new user" in {
      val fbUser = FbUser("id", Some("email"), "firstName", "name", Some("gender"))
      when(facebookService.fetchFacebookUser(any[String])(any[ExecutionContext])) thenReturn Future(Right(fbUser))

      val request = FakeRequest().withBody(Json.toJson(loginRequest))
      val response = userController.loginFacebookUser.apply(request)

      status(response) must equal(200)

      val responseBody = contentAs[UserLoginRegistrationResponse](response)
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
