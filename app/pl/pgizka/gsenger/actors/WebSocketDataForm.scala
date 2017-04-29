package pl.pgizka.gsenger.actors

import pl.pgizka.gsenger.Error
import play.api.libs.json.{JsValue, Json}

case class WebSocketRequest(method: String, id: Option[String], content: JsValue)

object WebSocketRequest {
  implicit val webSocketRequestForm = Json.format[WebSocketRequest]
}

case class WebSocketResponse(method: Option[String], id: Option[String], content: JsValue) {

  def this(requestContext: RequestContext, content: JsValue) = this(requestContext.method, requestContext.id, content)
}

case class WebSocketErrorResponse(method: Option[String], id: Option[String], status: Int,
                                  message: String, additionalInfo: Option[String]) {

  def this(method: Option[String], id: Option[String], error: Error) =
    this(method, id, error.code, error.message, error.info)

  def this(requestContext: RequestContext, error: Error) =
    this(requestContext.method, requestContext.id, error)

}

case class WebSocketPush[A](method: String, content: A)




