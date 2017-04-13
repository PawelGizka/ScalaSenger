package scala.controllers

import pl.pgizka.gsenger.core._
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import org.mockito.Mockito._
import org.specs2.mock.mockito.MockitoMatchers._
import pl.pgizka.gsenger.controllers.user.{UserController, UserFacebookLoginRequest, UserLoginRegistrationResponse}
import pl.pgizka.gsenger.services.facebook.{FacebookService, FbUser}

import scala.Utils._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class UserControllerSpec extends ControllerSpec {

  val facebookService = mock[FacebookService]
  val userController = new UserController(this, facebookService)

  val loginRequest = UserFacebookLoginRequest(123, "device id 2", "gcm token 2", "facebook token 2")

  before {
    db.run(create()).futureValue
  }

  after {
    db.run(drop()).futureValue
  }

  "loginFacebookUser" should {
    "return 200, new user id = 1 and access token when inserting new user" in {
      val fbUser = FbUser("id", Some("email"), "firstName", "name", Some("gender"))
      when(facebookService.fetchFacebookUser(any[String])) thenReturn Future(Right(fbUser))

      val request = FakeRequest().withBody(Json.toJson(loginRequest))
      val response = userController.loginFacebookUser.apply(request)

      status(response) must equal(200)

      val responseBody = contentAs[UserLoginRegistrationResponse](response)
      responseBody.userId must equal(1)
    }

    "return 400 when there is problem with fetching facebook user" in {
      when(facebookService.fetchFacebookUser(any[String])) thenReturn Future(Left("problem with access token"))

      val request = FakeRequest().withBody(Json.toJson(loginRequest))
      val response = userController.loginFacebookUser.apply(request)

      status(response) must equal(400)
    }
  }

}
