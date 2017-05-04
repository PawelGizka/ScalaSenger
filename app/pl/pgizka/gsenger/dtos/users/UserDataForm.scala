package pl.pgizka.gsenger.dtos.users

import pl.pgizka.gsenger.Utils.Js
import pl.pgizka.gsenger.model.User
import play.api.libs.json.Json



case class UserFacebookLoginRequestDto(
  phoneNumber: Int,
  deviceId: String,
  gcmToken: String,
  facebookToken: String)

object UserFacebookLoginRequestDto {
  implicit val userReqistrationRequestDtoFormat = Json.format[UserFacebookLoginRequestDto]
}

case class UserLoginRegistrationResponseDto(userId: Long, accessToken: String)

object UserLoginRegistrationResponseDto {
  implicit val userRegistrationResponseDtoFormat = Json.format[UserLoginRegistrationResponseDto]
}

case class ContactDto(
  id: Long,
  userName: Option[String],
  lastLoggedDate: Long,
  photoHash: Option[String],
  status: Option[String],
  phoneNumbers: List[Int]) {

  def this(user: User) = this(user.id.get.value, user.userName, user.lastLoggedDate, user.photoHash, user.status, List())
}

object ContactDto {
  implicit val contactDtoFormat: Js[ContactDto] = Json.format[ContactDto]
}

case class GetContactsRequestDto(phoneNumbers: List[Int])

object GetContactsRequestDto {
  implicit val getContactsRequestDtoFormat: Js[GetContactsRequestDto] = Json.format[GetContactsRequestDto]
}

case class GetContactsResponseDto(friends: Seq[ContactDto])

object GetContactsResponseDto {
  implicit val getContactsResponseDtoFormat: Js[GetContactsResponseDto] = Json.format[GetContactsResponseDto]
}
