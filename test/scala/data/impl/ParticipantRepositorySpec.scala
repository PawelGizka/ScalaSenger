package scala.data.impl

import java.time.{LocalDateTime, ZoneOffset}

import org.scalatest.BeforeAndAfter
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatest.time.{Minutes, Span}
import org.scalatestplus.play.PlaySpec
import pl.pgizka.gsenger.core.CreateChatRequest
import pl.pgizka.gsenger.model.Contact
import pl.pgizka.gsenger.persistance.H2DBConnector
import pl.pgizka.gsenger.persistance.impl.DAL

import scala.Utils.{testChat, testDevice, testParticipant, testUser}
import scala.data.BasicSpec


class ParticipantRepositorySpec extends BasicSpec {
  import scala.concurrent.ExecutionContext.Implicits.global
  import profile.api._

  val user1 = testUser(1)
  val user2 = testUser(2)
  val user3 = testUser(3)

  val chat1 = testChat(1)
  val chat2 = testChat(2)

  val participant1 = testParticipant(user1.id.get, chat1.id.get)
  val participant2 = testParticipant(user2.id.get, chat1.id.get)

  val participant3 = testParticipant(user1.id.get, chat2.id.get)
  val participant4 = testParticipant(user3.id.get, chat2.id.get)


  val userTestData = List(user1, user2, user3)
  val chatTestData = List(chat1, chat2)
  val participantTestData = List(participant1, participant2, participant3, participant4)

  before {
    db.run(DBIO.seq(
      schema.create,
      users ++= userTestData,
      chats ++= chatTestData,
      participants ++= participantTestData
    )).futureValue
  }

  after {
    db.run(schema.drop)
  }

  "insertFromChatRequest" should {
    "create and insert all participants from charRequest" in {
      val participantIds = Seq(user2.idValue, user3.idValue)
      val createChatRequest = CreateChatRequest(chat1.chatType, chat1.name, participantIds)

      val result = db.run(participants.insertFromCreateChatRequest(createChatRequest, chat1, user1)).futureValue

      result must have size 3
      result.foreach{ participant =>
        participant.chat must equal(chat1.id.get)
      }
    }
  }

  "findAllParticipants" should {
    "return all participants for specified chats" in {
      val result = db.run(participants.findAllParticipants(chatTestData)).futureValue

      result must have size 2
      result foreach{case (chat, participants) =>
        participants must have size 2
      }
    }
  }


}
