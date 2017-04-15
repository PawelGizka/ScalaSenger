package pl.pgizka.gsenger.persistance.impl

import java.time.Instant

import pl.pgizka.gsenger.controllers.user.UserFacebookLoginRequest
import pl.pgizka.gsenger.model.{User, UserId}
import pl.pgizka.gsenger.persistance.{EntityRepository, Profile}
import pl.pgizka.gsenger.services.facebook.FbUser
import slick.profile.SqlProfile.ColumnOption.Nullable

import scala.concurrent.ExecutionContext
//import scala.concurrent.ExecutionContext.Implicits.global

trait UserRepository extends EntityRepository {this: Profile =>

  import profile.api._

  class Users(tag: Tag) extends Table[User](tag, "Users") with EntityTable[UserId, User]{
    def id = column[UserId]("id", O.PrimaryKey, O.AutoInc)

    def userName = column[String]("user_name", Nullable)
    def email = column[String]("email", Nullable)
    def password = column[String]("password", Nullable)
    def lastLoggedDate = column[Long]("last_logged_date")
    def active = column[Boolean]("active")
    def photoHash = column[String]("photo_hash", Nullable)
    def status = column[String]("status", Nullable)
    def facebookId = column[String]("facebook_id", Nullable)
    def facebookToken = column[String]("facebook_token", Nullable)

    def * = (id.?, userName.?, email.?, password.?, lastLoggedDate, active, photoHash.?, status.?, facebookId.?, facebookToken.?) <> (User.tupled, User.unapply)
  }

  object users extends EntityQueries[UserId, User, Users](new Users(_)) {

    override def copyEntityFields(entity: User, id: Option[UserId]): User = entity.copy(id = id)

    def findByFacebookId(facebookId: String): DBIO[Option[User]] = users.filter(_.facebookId === facebookId).result.headOption

    def findByFacebookUsers(facebookUsers: Option[Seq[FbUser]])(implicit executionContext: ExecutionContext): DBIO[Option[Seq[User]]] = facebookUsers match {
      case Some(users) => findByFacebookUsers(users).map(Some(_))
      case None => DBIO.successful(None)
    }

    def findByFacebookUsers(facebookUsers: Seq[FbUser]): DBIO[Seq[User]] = findByFacebookIds(facebookUsers.map(_.id))

    def findByFacebookIds(facebookIds: Seq[String]): DBIO[Seq[User]] = users.filter(_.facebookId inSetBind facebookIds).result

    def insertFacebookUser(fbUser: FbUser, userFacebookLoginRequest: UserFacebookLoginRequest)(implicit ec: ExecutionContext): DBIO[User] = {
      findByFacebookId(fbUser.id).flatMap {
        case Some(user) => update(user.copy(facebookToken = Some(userFacebookLoginRequest.facebookToken)))
        case None => insert(new User(fbUser, userFacebookLoginRequest))
      }
    }
  }

}
