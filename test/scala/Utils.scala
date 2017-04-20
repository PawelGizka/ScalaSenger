package scala

import java.time.{LocalDateTime, ZoneOffset}

import pl.pgizka.gsenger.controllers.RestApiErrorResponse
import pl.pgizka.gsenger.model._
import play.api.libs.json.{Json, OFormat}
import play.api.mvc.Result
import play.api.test.Helpers._

import scala.concurrent.Future


object Utils {

  val time = LocalDateTime.of(2014, 2, 26, 9, 30)
  val inst = time.toInstant(ZoneOffset.UTC)

  def testUserWithToken(id: Long): (User, Token) = (testUser(id), testUserToken(UserId(id)))

  def testUser(id: Long)  = User(Some(UserId(id)), Some("user name " + id), Some("email " + id),
    Some("password " + id), inst.toEpochMilli, false, None, None, Some("facebook id " + id), Some("facebook token " + id))

  def testUserToken(id: UserId) = Token("some token " + id.value, id)

  def testDevice(id: Long, owner: User) = Device(Some(DeviceId(id)), "device id " + id, None, Some((id * 100).toInt), "gcm token " + id, owner.id.get)

  def testChat(id: Long) = Chat(Some(ChatId(id)), ChatType.groupChat, None, inst)

  def testParticipant(userId: UserId, chatId: ChatId) = Participant(None, userId, chatId, None, None)

  def testMessage(id: Long, sender: UserId, chatId: ChatId) =
    Message(Some(MessageId(id)), chatId, sender, id, "message text " + id)

  def contentAsErrorResponse: (Future[Result]) => RestApiErrorResponse = contentAs[RestApiErrorResponse]

  def contentAs[A](response: Future[Result])(implicit typeFormat: OFormat[A]) = Json.fromJson(contentAsJson(response))(typeFormat).get
}
