package pl.pgizka.gsenger.persistance.impl

import pl.pgizka.gsenger.Utils._
import pl.pgizka.gsenger.model._
import pl.pgizka.gsenger.persistance.Profile
import pl.pgizka.gsenger.services.facebook.FbUser
import slick.dbio.{DBIOAction, Effect, NoStream}
import slick.dbio.Effect.Write
import slick.lifted.PrimaryKey

import scala.collection.mutable
import scala.collection.mutable.Map
import scala.concurrent.{ExecutionContext, Future}

trait ContactRepository {this: UserRepository with DeviceRepository with Profile =>

  import profile.api._

  class Contacts(tag: Tag) extends Table[Contact](tag, "Contacts") {
    def userFromId = column[UserId]("user_from")
    def userToId = column[UserId]("user_to")
    def fromFacebook = column[Boolean]("from_facebook")
    def fromPhone = column[Boolean]("from_phone")

    def primaryKey: PrimaryKey = primaryKey("contact_pk", (userFromId, userToId))

    def userFrom = foreignKey("user_from_id_fk", userFromId, users)(_.id)
    def userTo = foreignKey("user_to_id_fk", userToId, users)(_.id)

    def * = (userFromId, userToId, fromFacebook, fromPhone) <> (Contact.tupled, Contact.unapply)
  }

  object contacts extends TableQuery(new Contacts(_)) {

    def list(): DBIO[Seq[Contact]] = contacts.result

    def findContacts(user: User): DBIO[Seq[(User, Contact)]] = findContacts(user.id.get)

    def findContacts(userId: UserId): DBIO[Seq[(User, Contact)]] = findContactsQuery(userId).result

    def findContactsQuery(userId: UserId) = {
      for {
        contact <- contacts
        if contact.userFromId === userId
        friend <- contact.userTo
      } yield (friend, contact)
    }

    def updateContacts(userFrom: User, fbUsers: Option[Seq[FbUser]], phoneNumbers: Seq[Int])
                      (implicit executionContext: ExecutionContext): DBIO[Seq[(User, Contact)]] = {
      for {
        existingContacts <- findContacts(userFrom)
        foundPhoneFriends <- devices.findFriendsByPhoneNumbers(phoneNumbers)
        foundFacebookFriends <- users.findByFacebookUsers(fbUsers)
        dbUpdate <- bulkInsertOrUpdate(updateContacts(userFrom, foundFacebookFriends, foundPhoneFriends, existingContacts))
        dbResult <- findContactsQuery(userFrom.id.get).result
      } yield dbResult
    }

    private def updateContacts(userFrom: User, foundFacebookFriends: Option[Seq[User]],
                foundPhoneFriends: Seq[User], existingContacts: Seq[(User, Contact)]): Seq[Contact] = {

      val fbUsersMap =
        if (foundFacebookFriends.isDefined) mutable.Map[Long, User](foundFacebookFriends.get.map(user => (user.idValue, user)): _*)
        else mutable.Map[Long, User]()

      def isFromFacebook(user: User, contact: Contact) =
        if (foundFacebookFriends.isDefined) fbUsersMap.get(user.idValue).isDefined else contact.fromFacebook

      val phoneUsersMap = mutable.Map[Long, User](foundPhoneFriends.map(user => (user.idValue, user)): _*)
      def isFromPhone(user: User) = phoneUsersMap.get(user.idValue).isDefined

      val updated: Seq[Contact] = existingContacts.map {
        case (user, contact) =>
          val updated = contact.copy(fromFacebook = isFromFacebook(user, contact), fromPhone = contact.fromPhone || isFromPhone(user))
          fbUsersMap.remove(user.idValue)
          phoneUsersMap.remove(user.idValue)
          updated
      }

      val updatedWithPhone = updated ++ phoneUsersMap.toList.map {
        case (_, user) => Contact(userFrom.id.get, user.id.get, fromFacebook = false, fromPhone = true)
      }

      val updatedWithPhoneAndFb = updatedWithPhone ++ fbUsersMap.toList.map {
        case (_, user) => Contact(userFrom.id.get, user.id.get, fromFacebook = true, fromPhone = false)
      }
      updatedWithPhoneAndFb
    }

    def bulkInsertOrUpdate(rows: Iterable[Contact]): DBIO[Iterable[Int]] = DBIO.sequence(rows.map(contacts.insertOrUpdate(_)))

    def ensureEverybodyKnowsEachOther(participants: Seq[Participant])(implicit executionContext: ExecutionContext) = {
      val rows: Seq[DBIO[Seq[Contact]]] = participants.map{participant =>
        findContacts(participant.user).map{actual =>
          getNotFoundElements(participants.map(_.user), actual.map(_._1.id.get)).map{ userId =>
              Contact(participant.user, userId, fromFacebook = false, fromPhone = false)
          }.filterNot(contact => contact.userFrom == participant.user && contact.userTo == participant.user)
        }
      }

      DBIO.sequence(rows.map{dbAction =>
        dbAction.flatMap{rows =>
          contacts ++= rows
        }
      })
    }
  }

}
