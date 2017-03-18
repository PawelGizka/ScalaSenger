package scala.data.impl

import java.time.{LocalDateTime, ZoneOffset}

import org.scalatest.BeforeAndAfter
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatest.time.{Minutes, Seconds, Span}
import org.scalatestplus.play.PlaySpec
import pl.pgizka.gsenger.core.FbUser
import pl.pgizka.gsenger.model._
import pl.pgizka.gsenger.persistance.H2DBConnector
import pl.pgizka.gsenger.persistance.impl.DAL
import scala.Utils._


class ContactsRepositorySpec extends PlaySpec with BeforeAndAfter with ScalaFutures with MockitoSugar
  with H2DBConnector with DAL {

  implicit override val patienceConfig = PatienceConfig(timeout = Span(5, Minutes))
  import scala.concurrent.ExecutionContext.Implicits.global

  import profile.api._
  val time = LocalDateTime.of(2014, 2, 26, 9, 30)
  val inst = time.toInstant(ZoneOffset.UTC)

  val userFrom = testUser(1)

  val userFriend1 = testUser(2)
  val userFriend2 = testUser(3)

  val otherUser1 = testUser(4)
  val otherUser2 = testUser(5)

  val otherUserDevice = testDevice(1, otherUser1)

  val contact1 = Contact(userFrom.id.get, userFriend1.id.get, fromFacebook = false, fromPhone = false)
  val contact2 = Contact(userFrom.id.get, userFriend2.id.get, fromFacebook = false, fromPhone = false)

  val userTestData = List(userFrom, userFriend1, userFriend2, otherUser1)
  val deviceTestData = List(otherUserDevice)
  val contactTestData = List(contact1, contact2)

  before {
    db.run(DBIO.seq(
      schema.create,
      users ++= userTestData,
      contacts ++= contactTestData,
      devices ++= deviceTestData
    )).futureValue
  }

  after {
    db.run(schema.drop)
  }

  "findContacts" should {
    "find all contacts for specified user" in {
        val foundContacts = db.run(contacts.findContacts(userFrom)).futureValue
        foundContacts must have size 2
    }
  }

  "updateContacts" should {
    "update Existing contacts when facebook users are provided" in {
      val fbUsers = Some(Seq(FbUser("facebook id 3", None, "firstName", "name", None)))
      val phoneNumbers = Seq(100)
      val updatedContacts = db.run(contacts.updateContacts(userFrom, fbUsers, phoneNumbers)).futureValue

      updatedContacts must have size 3
      val contactsMap = Map[UserId, (User, Contact)](updatedContacts.map(tuple => (tuple._1.id.get, tuple)):_*)
      val (user2, contact2) = contactsMap(UserId(2))
      val (user3, contact3) = contactsMap(UserId(3))
      val (user4, contact4) = contactsMap(UserId(4))

      contact2.fromFacebook must equal(false)
      contact3.fromFacebook must equal(true)
      contact4.fromFacebook must equal(false)

      contact2.fromPhone must equal(false)
      contact3.fromPhone must equal(false)
      contact4.fromPhone must equal(true)
    }
  }

}
