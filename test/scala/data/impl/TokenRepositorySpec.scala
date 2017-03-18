package scala.data.impl

import java.time.{LocalDateTime, ZoneOffset}

import org.scalatest.BeforeAndAfter
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatest.time.{Seconds, Span}
import org.scalatestplus.play.PlaySpec
import pl.pgizka.gsenger.model.{User, UserId, Version}
import pl.pgizka.gsenger.persistance.H2DBConnector
import pl.pgizka.gsenger.persistance.impl.DAL
import scala.Utils._

class TokenRepositorySpec extends PlaySpec with BeforeAndAfter with ScalaFutures with MockitoSugar
  with H2DBConnector with DAL {

  implicit override val patienceConfig = PatienceConfig(timeout = Span(5, Seconds))
  import scala.concurrent.ExecutionContext.Implicits.global

  import profile.api._
  val time = LocalDateTime.of(2014, 2, 26, 9, 30)
  val inst = time.toInstant(ZoneOffset.UTC)

  val user1 = testUser(1)

  before {
    db.run(DBIO.seq(
      schema.create,
      users += user1
    )).futureValue
  }

  after {
    db.run(schema.drop)
  }

  "generateTokens" should {
    "generate new token for user" in {
      val token = db.run(tokens.generateToken(user1.id.get)).futureValue
      token.userId must equal(user1.id.get)
    }
  }

}
