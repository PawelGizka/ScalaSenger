package pl.pgizka.gsenger.model

import java.time.Instant


case class FriendId(value: Long) extends EntityId(value)

case class Friend(
  id: Option[FriendId],
  version: Option[Version],
  created: Option[Instant],
  modified: Option[Instant],

  user1: Long,
  user2: Long,

  friendType: String) extends Entity[FriendId]
