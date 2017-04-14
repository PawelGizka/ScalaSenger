package pl.pgizka.gsenger.controllers

import play.api.libs.json.Json


case class Error(code: Int, message: String)

case class ErrorResponse(code: Int, message: String, additionalInfo: Option[String]) {
  def this(error: Error) = this(error.code, error.message, None)
  def this(error: Error, info: String) = this(error.code, error.message, Some(info))
}

object ErrorResponse {
  implicit val errorResponseFormat = Json.format[ErrorResponse]
}

object errors {
  val UserAlreadyExistsError = Error(1, "User already exists")
  val DatabaseError = Error(2, "Database error has occurred")
  val FetchFacebookDataError = Error(3, "Cannot fetch facebook data")
  val CouldNotFindUsersError = Error(4, "Could not find all users by specified ids")
  val CouldNotFindChatError = Error(5, "Could not find chat with specified id")
}
