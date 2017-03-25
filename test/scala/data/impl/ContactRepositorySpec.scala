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
import scala.data.RepositorySpec


class ContactRepositorySpec extends RepositorySpec{
  import scala.concurrent.ExecutionContext.Implicits.global
  import profile.api._

  val userFrom = testUser(1)

  val userFriend1 = testUser(2)
  val userFriend2 = testUser(3)

  val otherUser1 = testUser(4)
  val otherUser2 = testUser(5)

  val otherUserDevice = testDevice(1, otherUser1)

  val contact1 = Contact(userFrom.id.get, userFriend1.id.get, fromFacebook = false, fromPhone = false)
  val contact2 = Contact(userFrom.id.get, userFriend2.id.get, fromFacebook = false, fromPhone = false)
  val contact3 = Contact(userFriend1.id.get, userFriend2.id.get, fromFacebook = true, fromPhone = true)

  val chat = testChat(1)
  val participant1 = testParticipant(userFrom.id.get, chat.id.get)
  val participant2 = testParticipant(userFriend1.id.get, chat.id.get)
  val participant3 = testParticipant(userFriend2.id.get, chat.id.get)

  val userTestData = List(userFrom, userFriend1, userFriend2, otherUser1)
  val deviceTestData = List(otherUserDevice)
  val contactTestData = List(contact1, contact2, contact3)
  val chatTestData = List(chat)
  val participantTestData = List(participant1, participant2, participant3)

  before {
    db.run(DBIO.seq(
      schema.create,
      users ++= userTestData,
      contacts ++= contactTestData,
      devices ++= deviceTestData,
      chats ++= chatTestData,
      participants ++= participantTestData
    )).futureValue
  }

  after {
    db.run(schema.drop)
  }

  "findContacts" should {
    "find all contacts for specified user" in {
      db.run(contacts.findContacts(userFrom)).futureValue must have size 2

      db.run(contacts.findContacts(userFriend1)).futureValue must have size 1
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

  "ensureEverybodyKnowsEachOther" should {
    "insert when contact not exist and do nothing when contact exists" in {
      db.run(contacts.ensureEverybodyKnowsEachOther(participantTestData)).futureValue

      db.run(contacts.findContacts(userFrom)).futureValue must have size 2
      db.run(contacts.findContacts(userFriend2)).futureValue must have size 2

      val found = db.run(contacts.findContacts(userFriend1)).futureValue
      found must have size 2

      val contactsMap = Map[UserId, (User, Contact)](found.map(tuple => (tuple._1.id.get, tuple)):_*)
      val (user1, contact1) = contactsMap(UserId(1))
      val (user3, contact3) = contactsMap(UserId(3))

      contact1.fromFacebook must equal(false)
      contact1.fromPhone must equal(false)

      contact3.fromFacebook must equal(true)
      contact3.fromPhone must equal(true)
    }
  }

}
