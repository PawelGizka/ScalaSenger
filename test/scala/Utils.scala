package scala

import java.time.{LocalDateTime, ZoneOffset}

import pl.pgizka.gsenger.model._


object Utils {

  val time = LocalDateTime.of(2014, 2, 26, 9, 30)
  val inst = time.toInstant(ZoneOffset.UTC)

  def testUser(id: Long)  = User(Some(UserId(id)), Some(Version(0)), Some(inst), Some(inst), Some("user name " + id), Some("email " + id),
    Some("password " + id), inst.toEpochMilli, false, None, None, Some("facebook id " + id), Some("facebook token " + id))

  def testDevice(id: Long, owner: User) = Device(Some(DeviceId(id)), Some(Version(0)),
    Some(inst), Some(inst), "device id " + id, None, Some((id * 100).toInt), "gcm token " + id, owner.id.get)

}
