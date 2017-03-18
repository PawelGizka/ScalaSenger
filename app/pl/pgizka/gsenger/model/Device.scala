package pl.pgizka.gsenger.model

import java.time.Instant

import pl.pgizka.gsenger.core.UserFacebookLoginRequest

case class DeviceId(value: Long) extends EntityId(value)

case class Device(
  id: Option[DeviceId],
  version: Option[Version],
  created: Option[Instant],
  modified: Option[Instant],

  deviceId: String,
  appVersion: Option[String],
  phoneNumber: Option[Int],
  gcmPushToken: String,
  ownerId: UserId) extends Entity[DeviceId] {

  def this(owner: User, userFacebookLoginRequest: UserFacebookLoginRequest) =
    this (
      None,
      None,
      None,
      None,

      userFacebookLoginRequest.deviceId,
      None,
      Some(userFacebookLoginRequest.phoneNumber),
      userFacebookLoginRequest.gcmToken,
      owner.id.get
    )

}
