package pl.pgizka.gsenger

import java.time.{LocalDateTime, ZoneOffset}

import pl.pgizka.gsenger.model._
import play.api.libs.json.OFormat

import scala.collection.mutable


object Utils {

  def getNotFoundElements[A](toFind: Seq[A], found: Seq[A]): Seq[A] = {
    val map = mutable.Map[A, Boolean](toFind.map((_, true)): _*)
    found.map(map.remove)
    map.keySet.toSeq
  }

  def formatSequenceMessage[A](message: String, elements: Seq[A]): String =
    message + " " + elements.foldLeft("")((a, b) => a + ",  " + b)

  /**
    * Type alias for OFormat[A]
    * @tparam A
    */
  type Js[A] = OFormat[A]

  val time = LocalDateTime.of(2014, 2, 26, 9, 30)
  val inst = time.toInstant(ZoneOffset.UTC)

  def testUserWithToken(id: Long): (User, Token) = (testUser(id), testUserToken(UserId(id)))

  def testUser(id: Long)  = User(Some(UserId(id)), Some(Version(0)), Some(inst), Some(inst), Some("user name " + id), Some("email " + id),
    Some("password " + id), inst.toEpochMilli, false, None, None, Some("facebook id " + id), Some("facebook token " + id))

  def testUserToken(id: UserId) = Token("some token " + id.value, id)

  def testDevice(id: Long, owner: User) = Device(Some(DeviceId(id)), Some(Version(0)),
    Some(inst), Some(inst), "device id " + id, None, Some((id * 100).toInt), "gcm token " + id, owner.id.get)

  def testChat(id: Long) = Chat(Some(ChatId(id)), None, None, None, ChatType.groupChat, None, inst)

  def testParticipant(userId: UserId, chatId: ChatId) = Participant(None, None, None, None, userId, chatId, None, None)
}
