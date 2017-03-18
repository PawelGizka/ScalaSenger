package scala.controllers

import org.junit.Test
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatest.time.{Seconds, Span}
import org.scalatest.{BeforeAndAfter, FunSpec, FunSpecLike, ShouldMatchers}
import org.scalatestplus.play.PlaySpec
import org.specs2.execute.Results
import pl.pgizka.gsenger.core._
import pl.pgizka.gsenger.persistance.H2DBConnector
import pl.pgizka.gsenger.persistance.impl.DAL
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import org.mockito.Mockito._
import org.specs2.mock.mockito.MockitoMatchers._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class UserControllerSpec extends PlaySpec with Results with BeforeAndAfter with ScalaFutures with MockitoSugar
  with H2DBConnector with DAL {

  implicit val timeout = Span(5, Seconds)
  implicit override val patienceConfig = PatienceConfig(timeout)

  val facebookService = mock[FacebookService]
  val userController = new UserController(this, facebookService)

  before {
    db.run(create()).futureValue
  }

  after {
    db.run(drop()).futureValue
  }

  "loginFacebookUser" should {
    "return 200, new user id = 1 and access token when inserting new user" in {
      when(facebookService.fetchFacebookUser(any[String])) thenReturn Future(Right(FbUser("id", Some("email"), "firstName", "name", Some("gender"))))

      val request = FakeRequest().withBody(Json.toJson(UserFacebookLoginRequest(123, "device id 2", "gcm token 2", "facebook token 2")))
      val response = userController.loginFacebookUser.apply(request)

      status(response) must equal(200)

      val result = Json.fromJson(contentAsJson(response))(Json.format[UserLoginRegistrationResponse]).get
      result.userId must equal(1)
    }

    "return 400 when there is problem with fetching facebook user" in {
      when(facebookService.fetchFacebookUser(any[String])) thenReturn Future(Left("problem with access token"))

      val request = FakeRequest().withBody(Json.toJson(UserFacebookLoginRequest(123, "device id 2", "gcm token 2", "facebook token 2")))
      val response = userController.loginFacebookUser.apply(request)

      status(response) must equal(400)
    }
  }




}
