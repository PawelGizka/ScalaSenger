package pl.pgizka.gsenger.model

import java.time.Instant

import pl.pgizka.gsenger.core.{FbUser, UserFacebookLoginRequest}

case class UserId(value: Long) extends EntityId(value)

case class User(
  id: Option[UserId],
  version: Option[Version],
  created: Option[Instant],
  modified: Option[Instant],

  userName: Option[String],
  email: Option[String],
  password: Option[String],

  lastLoggedDate: Long,
  active: Boolean,
  photoHash: Option[String],
  status: Option[String],
  facebookId: Option[String],
  facebookToken: Option[String]) extends Entity[UserId] {

  def this (fbUser: FbUser, userFacebookLoginRequest: UserFacebookLoginRequest) =
    this(
      None,
      None,
      None,
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
