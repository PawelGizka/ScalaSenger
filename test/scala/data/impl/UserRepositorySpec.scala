package scala.data.impl


import pl.pgizka.gsenger.controllers.user.UserFacebookLoginRequest
import pl.pgizka.gsenger.model.UserId
import pl.pgizka.gsenger.services.facebook.FbUser

import scala.data.BasicSpecWithDefaultScenario


class UserRepositorySpec extends BasicSpecWithDefaultScenario {
  import scala.concurrent.ExecutionContext.Implicits.global
  import profile.api._

  import scala.data.DefaultScenario._

  "findByFacebookId" should {
    "should return None if no matching user with facebook id" in {
      db.run(users.findByFacebookId("facebook id 0")).futureValue must equal (None)
    }

    "should return some user with match facebook id" in {
      val result = db.run(users.findByFacebookId("facebook id 1")).futureValue
      result must be ('defined)
      result.get.id must equal (Some(UserId(1)))
    }
  }

  "insertFacebookUser" should {
    "should insert new record if the given facebookId is not found" in {
      val fbUser = new FbUser("facebook id 3")
      val loginRequest = UserFacebookLoginRequest(123, "deviceId", "gcmToken", "facebook token 3")
      val result = db.run(users.insertFacebookUser(fbUser, loginRequest)).futureValue

      result.facebookToken.get must equal ("facebook token 3")
    }

    "should update the existing record if the given facebookId exists" in {
      val fbUser = new FbUser("facebook id 1")
      val loginRequest = UserFacebookLoginRequest(123, "deviceId", "gcmToken", "facebook token 3")
      db.run(users.insertFacebookUser(fbUser, loginRequest)).futureValue

      val result = db.run(users.get(UserId(1))).futureValue
      result.facebookToken.get must equal ("facebook token 3")
    }
  }

}
