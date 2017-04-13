package scala.data.impl

import pl.pgizka.gsenger.model._
import pl.pgizka.gsenger.services.facebook.FbUser

import scala.data.{BasicSpec, BasicSpecWithDefaultScenario}


class ContactRepositorySpec extends BasicSpecWithDefaultScenario {
  import scala.concurrent.ExecutionContext.Implicits.global
  import profile.api._

  import scala.data.DefaultScenario._

  "findContacts" should {
    "find all contacts for specified user" in {
      db.run(contacts.findContacts(user1)).futureValue must have size 2

      db.run(contacts.findContacts(user2)).futureValue must have size 1
    }
  }

  "updateContacts" should {
    "update Existing contacts when facebook users are provided" in {
      val fbUsers = Some(Seq(FbUser("facebook id 3", None, "firstName", "name", None)))
      val phoneNumbers = Seq(400)
      val updatedContacts = db.run(contacts.updateContacts(user1, fbUsers, phoneNumbers)).futureValue

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

      db.run(contacts.findContacts(user1)).futureValue must have size 2
      db.run(contacts.findContacts(user3)).futureValue must have size 2

      val found = db.run(contacts.findContacts(user2)).futureValue
      found must have size 2

      val contactsMap = Map[UserId, (User, Contact)](found.map(tuple => (tuple._1.id.get, tuple)):_*)
      val (foundUser1, contact1) = contactsMap(UserId(1))
      val (foundUser3, contact3) = contactsMap(UserId(3))

      contact1.fromFacebook must equal(false)
      contact1.fromPhone must equal(false)

      contact3.fromFacebook must equal(true)
      contact3.fromPhone must equal(true)
    }
  }

}
