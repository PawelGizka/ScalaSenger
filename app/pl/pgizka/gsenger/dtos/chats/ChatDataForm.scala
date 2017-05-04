package pl.pgizka.gsenger.dtos.chats

import java.time.Instant

import pl.pgizka.gsenger.Utils.Js
import pl.pgizka.gsenger.model._
import play.api.libs.json.Json


case class CreateChatRequestDto(chatType: String, name: Option[String], participants: Seq[UserId])

object CreateChatRequestDto {
  implicit val createChatRequestFormat: Js[CreateChatRequestDto] = Json.format[CreateChatRequestDto]
}

case class ParticipantDto(id: ParticipantId, user: UserId, lastViewedMessage: Option[MessageId], messageViewedMessageDate: Option[Long]) {
  def this(participant: Participant) = this(participant.id.get, participant.user, participant.lastViewedMessage, participant.messageViewedMessageDate)
}

object ParticipantDto {
  implicit val participantInfoFormat: Js[ParticipantDto] = Json.format[ParticipantDto]
}

case class ChatDto(id: ChatId, chatType: String, name: Option[String], started: Instant, participantsInfos: Seq[ParticipantDto]) {
  def this(chat: Chat, participants: Seq[Participant]) = this(chat.id.get, chat.chatType, chat.name, chat.started, participants.map(new ParticipantDto(_)))
  def this(chatWithParticipants: (Chat, Seq[Participant])) = this(chatWithParticipants._1, chatWithParticipants._2)
}

object ChatDto {
  def apply(chatWithParticipants: (Chat, Seq[Participant])): ChatDto = this(chatWithParticipants._1, chatWithParticipants._2)
  def apply(chat: Chat, participants: Seq[Participant]): ChatDto = this(chat.id.get, chat.chatType, chat.name, chat.started, participants.map(new ParticipantDto(_)))

  implicit val chatInfoFormat: Js[ChatDto] = Json.format[ChatDto]
}

case class GetAllChatsWithParticipantInfoDto(chats: Seq[ChatDto])

object GetAllChatsWithParticipantInfoDto {
  implicit val getAllChatsWithParticipantInfoResponseFormat: Js[GetAllChatsWithParticipantInfoDto] = Json.format[GetAllChatsWithParticipantInfoDto]
}

