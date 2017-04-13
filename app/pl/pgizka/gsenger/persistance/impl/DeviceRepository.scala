package pl.pgizka.gsenger.persistance.impl

import java.time.Instant

import pl.pgizka.gsenger.controllers.user.UserFacebookLoginRequest
import pl.pgizka.gsenger.model._
import pl.pgizka.gsenger.persistance.{EntityRepository, Profile}
import slick.profile.SqlProfile.ColumnOption.Nullable

import scala.concurrent.ExecutionContext

trait DeviceRepository extends EntityRepository {this: Profile with UserRepository =>

  import profile.api._

  class Devices(tag: Tag) extends Table[Device](tag, "Devices") with EntityTable[DeviceId, Device] {
    def id = column[DeviceId]("id", O.PrimaryKey, O.AutoInc)
    def version = column[Version]("version", Nullable)
    def created = column[Instant]("created", Nullable)
    def modified = column[Instant]("modified", Nullable)

    def deviceId = column[String]("device_id")
    def appVersion = column[String]("app_version", Nullable)
    def phoneNumber = column[Int]("phone_number", Nullable)
    def gcmPushToken = column[String]("gcm_push_token")
    def ownerId = column[UserId]("owner_id")

    def owner = foreignKey("owner_id_fk", ownerId, users)(_.id, ForeignKeyAction.Cascade, ForeignKeyAction.Cascade)

    def * = (id.?, version.?, created.?, modified.?, deviceId, appVersion.?, phoneNumber.?, gcmPushToken, ownerId) <> (Device.tupled, Device.unapply)
  }

  object devices extends EntityQueries[DeviceId, Device, Devices](new Devices(_)) {

    override def copyEntityFields(entity: Device, id: Option[DeviceId], version: Option[Version], created: Option[Instant], modified: Option[Instant]): Device =
      entity.copy(id = id, version = version, created = created, modified = modified)

    def findByDeviceId(deviceId: String): DBIO[Option[Device]] = devices.filter(_.deviceId === deviceId).result.headOption

    def insertIfNecessary(owner: User, userFacebookLoginRequest: UserFacebookLoginRequest)(implicit ec: ExecutionContext): DBIO[Device] = {
      findByDeviceId(userFacebookLoginRequest.deviceId).flatMap{
        case Some(device) => update(device.copy(gcmPushToken = userFacebookLoginRequest.gcmToken))
        case None => insert(new Device(owner, userFacebookLoginRequest))
      }
    }

    def findFriendsByPhoneNumbers(phoneNumbers: Seq[Int]): DBIO[Seq[User]]= {
      (for {
        device <- devices
        if device.phoneNumber inSetBind phoneNumbers
        owner <- device.owner
      } yield owner).result
    }

  }

}
