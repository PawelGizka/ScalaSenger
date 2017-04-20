package pl.pgizka.gsenger.controllers

import pl.pgizka.gsenger.Error
import play.api.libs.json.Json


case class RestApiErrorResponse(code: Int, message: String, additionalInfo: Option[String]) {
  def this(error: Error) = this(error.code, error.message, error.info)
}

object RestApiErrorResponse {
  implicit val errorResponseFormat = Json.format[RestApiErrorResponse]
}
