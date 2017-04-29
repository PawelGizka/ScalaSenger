package pl.pgizka.gsenger.actors

import pl.pgizka.gsenger.model.User


case class RequestContext(id: Option[String] = None, method: Option[String] = None, user: Option[User] = None) {
  def this(webSocketRequest: WebSocketRequest) = this(webSocketRequest.id, Some(webSocketRequest.method))
}

case class ActorResponse[A](content: A, requestContext: RequestContext = RequestContext())