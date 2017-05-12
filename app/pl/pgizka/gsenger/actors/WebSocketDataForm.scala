package pl.pgizka.gsenger.actors

import pl.pgizka.gsenger.Error
import play.api.libs.json.{JsPath, JsValue, Json, Writes}
import play.api.libs.json._
import play.api.libs.functional.syntax._


case class WebSocketRequest(method: String, id: Option[String], content: JsValue)

object WebSocketRequest {
  implicit val webSocketRequestForm = Json.format[WebSocketRequest]
}

case class WebSocketResponse(method: Option[String], id: Option[String], content: JsValue) {

  def this(requestContext: RequestContext, content: JsValue) = this(requestContext.method, requestContext.id, content)
}

object WebSocketResponse {
  implicit val webSocketErrorResponseFormat = Json.format[WebSocketResponse]
}

case class WebSocketErrorResponse(method: Option[String], id: Option[String], status: Int,
                                  message: String, additionalInfo: Option[String]) {

  def this(method: Option[String], id: Option[String], error: Error) =
    this(method, id, error.code, error.message, error.info)

  def this(requestContext: RequestContext, error: Error) =
    this(requestContext.method, requestContext.id, error)

  def this(webSocketRequest: WebSocketRequest, error: Error) = {
    this(Some(webSocketRequest.method), webSocketRequest.id, error)
  }
}

object WebSocketErrorResponse {
  implicit val webSocketErrorResponseFormat = Json.format[WebSocketErrorResponse]
}

case class WebSocketPush(method: String, content: JsValue)

object WebSocketPush {
  implicit val webSocketPushFormat = Json.format[WebSocketPush]
}



