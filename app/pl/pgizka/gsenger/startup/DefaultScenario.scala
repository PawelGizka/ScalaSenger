package pl.pgizka.gsenger.startup

import java.time.{LocalDateTime, ZoneOffset}

import pl.pgizka.gsenger.model._
import pl.pgizka.gsenger.Utils._
import pl.pgizka.gsenger.persistance.DatabaseSupport
import pl.pgizka.gsenger.persistance.impl.DAL
import slick.dbio.Effect.{Schema, Write}
import slick.dbio.{DBIOAction, NoStream}


object DefaultScenario {

  val time = LocalDateTime.of(2014, 2, 26, 9, 30)
  val inst = time.toInstant(ZoneOffset.UTC)

  val (user1, token1) = testUserWithToken(1)
  val (user2, token2) = testUserWithToken(2)
  val (user3, token3) = testUserWithToken(3)
  val (user4, token4) = testUserWithToken(4)
  val (user5, token5) = testUserWithToken(5)

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

  val message1 = testMessage(1, user1.id.get, chat1.id.get)
  val message2 = testMessage(2, user1.id.get, chat1.id.get)
  val message3 = testMessage(3, user1.id.get, chat1.id.get)

  val userTestData = List(user1, user2, user3, user4, user5)
  val tokenTestData = List(token1, token2, token3, token4, token5)
  val deviceTestData = List(device1, device2, device3, device4, device5)
  val contactTestData = List(contact1, contact2, contact3)
  val chatTestData = List(chat1, chat2)
  val participantTestData = List(participant1, participant2, participant3, participant4)
  val messageTestData = List(message1, message2, message3)

  def createDefaultScenarioAction(dataAccess: DAL with DatabaseSupport): DBIOAction[Unit, NoStream, Write with Schema] = {
    import dataAccess._
    import profile.api._

    DBIO.seq(
      schema.create,
      users ++= userTestData,
      tokens ++= tokenTestData,
      devices ++= deviceTestData,
      contacts ++= contactTestData,
      chats ++= chatTestData,
      participants ++= participantTestData,
      messages ++= messageTestData
    )
  }

  def testUserWithToken(id: Long): (User, Token) = (testUser(id), testUserToken(UserId(id)))

  def testUser(id: Long)  = User(Some(UserId(id)), Some("user name " + id), Some("email " + id),
    Some("password " + id), inst.toEpochMilli, false, None, None, Some("facebook id " + id), Some("facebook token " + id))

  def testUserToken(id: UserId) = Token("some token " + id.value, id)

  def testDevice(id: Long, owner: User) = Device(Some(DeviceId(id)), "device id " + id, None, Some((id * 100).toInt), "gcm token " + id, owner.id.get)

  def testChat(id: Long) = Chat(Some(ChatId(id)), ChatType.groupChat, None, inst)

  def testParticipant(userId: UserId, chatId: ChatId) = Participant(None, userId, chatId, None, None)

  def testMessage(id: Long, sender: UserId, chatId: ChatId) =
    Message(Some(MessageId(id)), chatId, sender, id, "message text " + id)



}
