package pl.pgizka.gsenger.core

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
  val UserAlreadyExists = Error(1, "User already exists")
  val DatabaseError = Error(2, "Database error has occurred")
  val FetchFacebookDataError = Error(3, "Cannot fetch facebook data")
}
