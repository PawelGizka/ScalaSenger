package scala.data.impl

import java.time.{LocalDateTime, ZoneOffset}

import pl.pgizka.gsenger.core.UserFacebookLoginRequest
import pl.pgizka.gsenger.model._
import org.scalatest.{BeforeAndAfter, FunSpec, ShouldMatchers}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}
import org.scalatestplus.play.PlaySpec
import pl.pgizka.gsenger.persistance.H2DBConnector
import pl.pgizka.gsenger.persistance.impl.{DeviceRepository, UserRepository}

import scala.Utils._
import scala.data.RepositorySpec

class DeviceRepositorySpec extends RepositorySpec {
  import scala.concurrent.ExecutionContext.Implicits.global
  import profile.api._

  val user1 = testUser(1)

  val user2 = testUser(2)

  val device = testDevice(1, user1)

  before {
    db.run(DBIO.seq(
      schema.create,
      users += user1,
      users += user2,
      devices += device
    )).futureValue
  }

  after {
    db.run(schema.drop)
  }

  "list" should {
    "should return all entities in table" in {
      val result = db.run(devices.list()).futureValue
      result must have length(1)
      result.head must equal(device)
    }
  }

  "findByDeviceId" should {
    "should return None if no matching device with device id" in {
      db.run(devices.findByDeviceId("device id 2")).futureValue must equal (scala.None)
    }

    "should return some device with match device id" in {
      val result = db.run(devices.findByDeviceId("device id 1")).futureValue
      result must be ('defined)
      result.get must be (device)
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
