package pl.pgizka.gsenger.actors

import pl.pgizka.gsenger.Error

case class WebSocketRequest[A](method: String, id: Option[String], content: A)

case class WebSocketResponse[A](method: String, id: Option[String], content: A)

case class WebSocketErrorResponse(method: String, id: Option[String], status: Int,
                                  message: String, additionalInfo: String) {

  def this(method: String, id: Option[String], error: Error, additionalInfo: String) =
    this(method, id, error.code, error.message, additionalInfo)

}

case class WebSocketPush[A](method: String, content: A)




