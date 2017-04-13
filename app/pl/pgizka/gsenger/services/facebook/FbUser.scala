package pl.pgizka.gsenger.services.facebook

case class FbUser(id: String, email: Option[String], first_name: String, name: String, gender: Option[String]) {
  def this(id: String) = this(id, None, "firstName", "name", None)
}

case class FbUserPictureData(url: Option[String])
case class FbUserPicture(data: Option[FbUserPictureData])
