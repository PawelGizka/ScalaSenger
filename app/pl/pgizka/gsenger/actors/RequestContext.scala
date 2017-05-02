package pl.pgizka.gsenger.actors

import pl.pgizka.gsenger.model.User
import pl.pgizka.gsenger.Error

case class RequestContext(id: Option[String] = None, method: Option[String] = None, user: Option[User] = None) {
  def this(webSocketRequest: WebSocketRequest) = this(webSocketRequest.id, Some(webSocketRequest.method))
}

trait ActorResponse {
  val requestContext: RequestContext = RequestContext()
}

case class ActorErrorResponse(error: Error, override val requestContext: RequestContext) extends ActorResponse