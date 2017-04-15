package pl.pgizka.gsenger.model

import pl.pgizka.gsenger.Utils.Js
import play.api.libs.json.{Json, OFormat}

case class ParticipantId(value: Long) extends EntityId(value)

object ParticipantId {
  implicit val participantIdFormat: Js[ParticipantId] = Json.format[ParticipantId]
}

case class Participant(
  id: Option[ParticipantId],

  user: UserId,
  chat: ChatId,
  lastViewedMessage: Option[MessageId],
  messageViewedMessageDate: Option[Long]) extends Entity[ParticipantId] {

  def this(chat: Chat, userId: UserId) =
    this(
      None,

      userId,
      chat.id.get,
      None,
      None
    )
}
