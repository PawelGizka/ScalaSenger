package scala.data.impl

import java.time.{LocalDateTime, ZoneOffset}

import pl.pgizka.gsenger.core.{FbUser, UserFacebookLoginRequest}
import pl.pgizka.gsenger.model.{User, UserId, Version}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}
import org.scalatest.{BeforeAndAfter, FunSpec, ShouldMatchers, TestData}
import org.scalatestplus.play.PlaySpec
import pl.pgizka.gsenger.persistance.H2DBConnector
import pl.pgizka.gsenger.persistance.impl.UserRepository
import scala.Utils._

import scala.data.{TestEntity, TestEntityId}


class UserRepositorySpec extends PlaySpec with BeforeAndAfter
  with ScalaFutures with H2DBConnector with UserRepository {

  implicit override val patienceConfig = PatienceConfig(timeout = Span(5, Seconds))
  import scala.concurrent.ExecutionContext.Implicits.global

  import profile.api._
  val time = LocalDateTime.of(2014, 2, 26, 9, 30)
  val inst = time.toInstant(ZoneOffset.UTC)

  val testData = List(
    testUser(1),
    testUser(2)
  )

  before {
    db.run(DBIO.seq(
      users.schema.create,
      users ++= testData
    )).futureValue
  }

  after {
    db.run(users.schema.drop)
  }

  "list" should {
    "should return all entities in the table" in {
      db.run(users.list()).futureValue must have length (2)
    }
  }

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
