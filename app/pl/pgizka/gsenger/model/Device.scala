package pl.pgizka.gsenger.model

import pl.pgizka.gsenger.dtos.users.UserFacebookLoginRequestDto

case class DeviceId(value: Long) extends EntityId(value)

case class Device(
  id: Option[DeviceId],

  deviceId: String,
  appVersion: Option[String],
  phoneNumber: Option[Int],
  gcmPushToken: String,
  ownerId: UserId) extends Entity[DeviceId] {

  def this(owner: User, userFacebookLoginRequest: UserFacebookLoginRequestDto) =
    this (
      None,

      userFacebookLoginRequest.deviceId,
      None,
      Some(userFacebookLoginRequest.phoneNumber),
      userFacebookLoginRequest.gcmToken,
      owner.id.get
    )

}
