package scala.data.impl

import org.scalatest.BeforeAndAfter
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatest.time.{Minutes, Span}
import org.scalatestplus.play.PlaySpec
import pl.pgizka.gsenger.core.CreateChatRequest
import pl.pgizka.gsenger.persistance.H2DBConnector
import pl.pgizka.gsenger.persistance.impl.DAL

import scala.Utils._
import scala.data.RepositorySpec


class ChatsRepositorySpec extends RepositorySpec {

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

  "findAllChats" should {
    "return all chats for specified user" in {
      val foundChats = db.run(chats.findAllChats(user1.id.get)).futureValue

      foundChats must have size 2
    }
  }


}
