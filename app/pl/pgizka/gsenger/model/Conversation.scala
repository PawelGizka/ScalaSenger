package pl.pgizka.gsenger.model

import java.time.Instant

case class ConversationId(value: Long) extends EntityId(value)

case class Conversation(
  id: Option[ConversationId],
  version: Option[Version],
  created: Option[Instant],
  modified: Option[Instant],

  conversationType: String,
  started: Long) extends Entity[ConversationId]
