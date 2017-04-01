package pl.pgizka.gsenger.core

import java.time.Instant

import pl.pgizka.gsenger.Utils.Js
import pl.pgizka.gsenger.model._
import play.api.libs.json.{JsPath, Json, OFormat, Writes}

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

case class CreateChatRequest(chatType: String, name: Option[String], participants: Seq[Long])

object CreateChatRequest {
  implicit val createChatRequestFormat: Js[CreateChatRequest] = Json.format[CreateChatRequest]
}

case class CreateChatResponse(chatId: Long)

object CreateChatResponse {
  implicit val createChatResponseFormat: Js[CreateChatResponse] = Json.format[CreateChatResponse]
}

case class ParticipantInfo(id: ParticipantId, user: UserId, lastViewedMessage: Option[MessageId], messageViewedMessageDate: Option[Long]) {
  def this(participant: Participant) = this(participant.id.get, participant.user, participant.lastViewedMessage, participant.messageViewedMessageDate)
}

object ParticipantInfo {
  implicit val participantInfoFormat: Js[ParticipantInfo] = Json.format[ParticipantInfo]
}

case class ChatInfo(id: ChatId, chatType: String, name: Option[String], started: Instant, participantsInfos: Seq[ParticipantInfo]) {
  def this(chat: Chat, participants: Seq[Participant]) = this(chat.id.get, chat.chatType, chat.name, chat.started, participants.map(new ParticipantInfo(_)))
  def this(chatWithParticipants: (Chat, Seq[Participant])) = this(chatWithParticipants._1, chatWithParticipants._2)
}

object ChatInfo {
  def apply(chatWithParticipants: (Chat, Seq[Participant])): ChatInfo = this(chatWithParticipants._1, chatWithParticipants._2)
  def apply(chat: Chat, participants: Seq[Participant]): ChatInfo = this(chat.id.get, chat.chatType, chat.name, chat.started, participants.map(new ParticipantInfo(_)))

  implicit val chatInfoFormat: Js[ChatInfo] = Json.format[ChatInfo]
}

case class ListAllChatsWithParticipantInfoResponse(chats: Seq[ChatInfo])

object ListAllChatsWithParticipantInfoResponse {
  implicit val listAllChatsWithParticipantInfoResponseFormat: Js[ListAllChatsWithParticipantInfoResponse] = Json.format[ListAllChatsWithParticipantInfoResponse]
}

case class CreateMessageRequest(chatId: ChatId, text: String)

object CreateMessageRequest {
  implicit val createMessageRequestFormat: Js[CreateMessageRequest] = Json.format[CreateMessageRequest]
}





