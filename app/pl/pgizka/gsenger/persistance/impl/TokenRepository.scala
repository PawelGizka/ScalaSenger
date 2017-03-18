package pl.pgizka.gsenger.persistance.impl

import java.security.SecureRandom

import pl.pgizka.gsenger.model.{Token, User, UserId}
import pl.pgizka.gsenger.persistance.{EntityRepository, Profile}
import java.util.Base64

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global

trait TokenRepository extends EntityRepository {this: Profile with UserRepository =>

  import profile.api._

  class Tokens(tag: Tag) extends Table[Token](tag, "Tokens") {
    def token = column[String]("token")
    def userId = column[UserId]("user_id_fk")

    def tokenUniqueIdx = index("token_unq", token, unique = true)
    def userFk = foreignKey("tokens_user_fk", userId, users)(_.id)

    def * = (token, userId) <> (Token.tupled, Token.unapply)
  }

  object tokens extends TableQuery(new Tokens(_)) {

    def generateToken(userId: UserId)(implicit ec: ExecutionContext): DBIO[Token] = {
      val token = Token(randomString(), userId)
      (tokens += token).map(_ => token)
    }

    private val secRandom = SecureRandom.getInstance("SHA1PRNG")
    private val encoder = Base64.getUrlEncoder

    private def randomString() = {
      val bytes = new Array[Byte](64)
      secRandom.nextBytes(bytes)
      val token = encoder.encode(bytes)
      new String(token).replaceAll("=", "")
    }

    def findUser(token: String): DBIO[Option[User]] = {
      tokens.filter(_.token === token).result.headOption.flatMap{
        case Some(found) => users.find(found.userId)
        case None => DBIO.successful(None)
      }
    }

  }

}
