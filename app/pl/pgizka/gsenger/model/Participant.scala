package pl.pgizka.gsenger.model

import java.time.Instant

case class ParticipantId(value: Long) extends EntityId(value)

case class Participant(
  id: Option[ParticipantId],
  version: Option[Version],
  created: Option[Instant],
  modified: Option[Instant],

  user: UserId,
  chat: ChatId,
  lastViewedMessage: Option[MessageId],
  messageViewedMessageDate: Option[Long]) extends Entity[ParticipantId] {

  def this(chat: Chat, userId: UserId) =
    this(
      None,
      None,
      None,
      None,
      userId,
      chat.id.get,
      None,
      None
    )
}
