package pl.pgizka.gsenger.controllers.user

import pl.pgizka.gsenger.Utils.Js
import pl.pgizka.gsenger.model.User
import play.api.libs.json.Json



case class UserFacebookLoginRequest(
  phoneNumber: Int,
  deviceId: String,
  gcmToken: String,
  facebookToken: String)

object UserFacebookLoginRequest {
  implicit val userReqistrationRequestFormat = Json.format[UserFacebookLoginRequest]
}

case class UserLoginRegistrationResponse(userId: Long, accessToken: String)

object UserLoginRegistrationResponse {
  implicit val userRegistrationResponseFormat = Json.format[UserLoginRegistrationResponse]
}

case class Friend(
  id: Long,
  userName: Option[String],
  lastLoggedDate: Long,
  photoHash: Option[String],
  status: Option[String],
  phoneNumbers: List[Int]) {

  def this(user: User) = this(user.id.get.value, user.userName, user.lastLoggedDate, user.photoHash, user.status, List())
}

object Friend {
  implicit val friendFormat: Js[Friend] = Json.format[Friend]
}

case class GetFriendsRequest(phoneNumbers: List[Int])

object GetFriendsRequest {
  implicit val getFriendsRequestFormat: Js[GetFriendsRequest] = Json.format[GetFriendsRequest]
}

case class GetFriendsResponse(friends: Seq[Friend])

object GetFriendsResponse {
  implicit val getFriendsResponseFormat: Js[GetFriendsResponse] = Json.format[GetFriendsResponse]
}
