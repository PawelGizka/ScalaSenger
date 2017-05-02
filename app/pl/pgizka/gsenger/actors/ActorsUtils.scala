package pl.pgizka.gsenger.actors

import akka.actor.ActorRef
import pl.pgizka.gsenger.Error
import pl.pgizka.gsenger.controllers.RestApiErrorResponse
import pl.pgizka.gsenger.errors.DatabaseError
import play.api.Logger
import play.api.libs.json.Json.toJson
import play.api.mvc.Result

import scala.util.{Failure, Success, Try}


object ActorsUtils {

  def handleDbComplete(sender: ActorRef, requestContext: RequestContext = RequestContext())(onSuccess: Any => Any): Try[Any] => Any = {
    case Success(error: Error) => sender ! ActorErrorResponse(error, requestContext)
    case Failure(throwable) => sender ! ActorErrorResponse(DatabaseError(throwable.getMessage), requestContext)
    case Success(result) => onSuccess(result)
  }

  def databaseError(webSocketRequest: WebSocketRequest): PartialFunction[scala.Throwable, WebSocketErrorResponse] = {
    case e =>
      val logger: Logger = Logger("actors")
      logger.error("Database error occurred", e)
      new WebSocketErrorResponse(webSocketRequest, DatabaseError(e.getMessage))
  }

}
