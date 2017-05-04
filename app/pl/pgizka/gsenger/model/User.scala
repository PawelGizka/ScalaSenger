package pl.pgizka.gsenger.model

import java.time.Instant

import pl.pgizka.gsenger.Utils.Js
import pl.pgizka.gsenger.dtos.users.UserFacebookLoginRequestDto
import pl.pgizka.gsenger.services.facebook.FbUser
import play.api.libs.json.{Json, OFormat}

case class UserId(value: Long) extends EntityId(value)

object UserId {
  implicit val userIdFormat: Js[UserId] = Json.format[UserId]
}

case class User(
  id: Option[UserId],

  userName: Option[String],
  email: Option[String],
  password: Option[String],

  lastLoggedDate: Long,
  active: Boolean,
  photoHash: Option[String],
  status: Option[String],
  facebookId: Option[String],
  facebookToken: Option[String]) extends Entity[UserId] {

  def this (fbUser: FbUser, userFacebookLoginRequest: UserFacebookLoginRequestDto) =
    this(
      None,

      Some(fbUser.first_name + fbUser.name),
      fbUser.email,
      None,
      Instant.now().toEpochMilli(),
      false,
      None,
      None,
      Some(fbUser.id),
      Some(userFacebookLoginRequest.facebookToken)
    )

  def idValue: Long = id.get.value

}
