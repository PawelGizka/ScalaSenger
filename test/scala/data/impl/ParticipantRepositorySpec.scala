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
import scala.data.RepositorySpec


class ParticipantRepositorySpec extends RepositorySpec {
  import scala.concurrent.ExecutionContext.Implicits.global
  import profile.api._

  val user1 = testUser(1)
  val user2 = testUser(2)
  val user3 = testUser(3)
  val chat = testChat(1)

  val participantIds = Seq(user2.idValue, user3.idValue)

  val createChatRequest = CreateChatRequest(chat.chatType, chat.name, participantIds)

  val userTestData = List(user1, user2, user3)
  val chatTestData = List(chat)

  before {
    db.run(DBIO.seq(
      schema.create,
      users ++= userTestData,
      chats ++= chatTestData
    )).futureValue
  }

  after {
    db.run(schema.drop)
  }

  "insertFromChatRequest" should {
    "create and insert all participants from charRequest" in {
      val result = db.run(participants.insertFromCreateChatRequest(createChatRequest, chat, user1)).futureValue

      result must have size 3
      result.foreach{ participant =>
        participant.chat must equal(chat.id.get)
      }
    }
  }


}
