package scala.data.impl

import pl.pgizka.gsenger.controllers.user.UserFacebookLoginRequest
import pl.pgizka.gsenger.model._

import scala.data.{BasicSpec, BasicSpecWithDefaultScenario}

class DeviceRepositorySpec extends BasicSpecWithDefaultScenario {
  import scala.concurrent.ExecutionContext.Implicits.global
  import profile.api._

  import scala.data.DefaultScenario._

  "findByDeviceId" should {
    "should return None if no matching device with device id" in {
      db.run(devices.findByDeviceId("device id 211")).futureValue must equal (scala.None)
    }

    "should return some device with match device id" in {
      val result = db.run(devices.findByDeviceId("device id 1")).futureValue
      result must be ('defined)
      result.get must be (device1)
    }
  }

  "insertIfNecessary" should {
    "should insert new record if the given deviceId is not found" in {
      val loginRequest = UserFacebookLoginRequest(100, "device id 2", "gcm token 2", "facebook token 2")
      db.run(devices.insertIfNecessary(user2, loginRequest)).futureValue.gcmPushToken must equal ("gcm token 2")
    }

    "should update the existing record if the given deviceId exists" in {
      val loginRequest = UserFacebookLoginRequest(100, "device id 1", "gcm token 3", "facebook token 2")
      val result = db.run(devices.insertIfNecessary(user2, loginRequest)).futureValue

      result.id.get must equal (DeviceId(1))
      result.gcmPushToken must equal ("gcm token 3")
    }
  }

  "findFriendsByPhoneNumbers" should {
    "return some users when there is matching user device" in {
      val phoneNumbers = Seq(100)
      val users = db.run(devices.findFriendsByPhoneNumbers(phoneNumbers)).futureValue
      users must have size 1
    }

    "return nothing when there is no matching user device" in {
      val phoneNumbers = Seq(101)
      val users = db.run(devices.findFriendsByPhoneNumbers(phoneNumbers)).futureValue
      users must have size 0
    }
  }

}
