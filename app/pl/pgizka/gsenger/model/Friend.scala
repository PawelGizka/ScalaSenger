package pl.pgizka.gsenger.model

case class FriendId(value: Long) extends EntityId(value)

case class Friend(
  id: Option[FriendId],

  user1: Long,
  user2: Long,

  friendType: String) extends Entity[FriendId]
