package pl.pgizka.gsenger.persistance.impl

import pl.pgizka.gsenger.core.FbUser
import pl.pgizka.gsenger.model.{Contact, ContactUserWrapper, User, UserId}
import pl.pgizka.gsenger.persistance.Profile
import slick.dbio.DBIOAction
import slick.dbio.Effect.Write
import slick.lifted.PrimaryKey

import scala.collection.mutable
import scala.collection.mutable.Map
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

trait ContactsRepository {this: UserRepository with DeviceRepository with Profile =>

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

    def findContacts(user: User): DBIO[Seq[(User, Contact)]] = findContactsQuery(user).result

    def findContactsQuery(user: User) = {
      for {
        contact <- contacts
        if contact.userFromId === user.id.get
        friend <- contact.userTo
      } yield (friend, contact)
    }

    def updateContacts(userFrom: User, fbUsers: Option[Seq[FbUser]], phoneNumbers: Seq[Int]): DBIO[Seq[(User, Contact)]] = {
      for {
        existingContacts <- findContacts(userFrom)
        foundPhoneFriends <- devices.findFriendsByPhoneNumbers(phoneNumbers)
        foundFacebookFriends <- users.findByFacebookUsers(fbUsers)
        dbUpdate <- bulkInsertOrUpdate(updateContacts(userFrom, foundFacebookFriends, foundPhoneFriends, existingContacts))
        dbResult <- findContactsQuery(userFrom).result
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
  }
}
