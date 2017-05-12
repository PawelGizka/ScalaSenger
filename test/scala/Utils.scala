package scala

import pl.pgizka.gsenger.controllers.RestApiErrorResponse
import play.api.libs.json.{Json, OFormat}
import play.api.mvc.Result
import play.api.test.Helpers._

import scala.concurrent.Future


object Utils {

  def contentAsErrorResponse: (Future[Result]) => RestApiErrorResponse = contentAs[RestApiErrorResponse]

  def contentAs[A](response: Future[Result])(implicit typeFormat: OFormat[A]) = Json.fromJson(contentAsJson(response))(typeFormat).get
}
