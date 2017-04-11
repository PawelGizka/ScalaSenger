package scala.data

import pl.pgizka.gsenger.model.Contact

import scala.Utils._


object DefaultScenario {

  val user1 = testUser(1)
  val user2 = testUser(2)
  val user3 = testUser(3)
  val user4 = testUser(4)
  val user5 = testUser(5)

  val device1 = testDevice(1, user1)
  val device2 = testDevice(2, user2)
  val device3 = testDevice(3, user3)
  val device4 = testDevice(4, user4)
  val device5 = testDevice(5, user5)

  val contact1 = Contact(user1.id.get, user2.id.get, fromFacebook = false, fromPhone = false)
  val contact2 = Contact(user1.id.get, user3.id.get, fromFacebook = false, fromPhone = false)
  val contact3 = Contact(user2.id.get, user3.id.get, fromFacebook = true, fromPhone = true)

  val chat1 = testChat(1)
  val chat2 = testChat(2)

  val participant1 = testParticipant(user1.id.get, chat1.id.get)
  val participant2 = testParticipant(user2.id.get, chat1.id.get)

  val participant3 = testParticipant(user1.id.get, chat2.id.get)
  val participant4 = testParticipant(user3.id.get, chat2.id.get)


  val userTestData = List(user1, user2, user3, user4, user5)
  val deviceTestData = List(device1, device2, device3, device4, device5)
  val contactTestData = List(contact1, contact2, contact3)
  val chatTestData = List(chat1, chat2)
  val participantTestData = List(participant1, participant2, participant3, participant4)

}
