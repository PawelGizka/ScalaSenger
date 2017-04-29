package pl.pgizka.gsenger.actors

import akka.actor.ActorRef
import pl.pgizka.gsenger.Error
import pl.pgizka.gsenger.errors.DatabaseError

import scala.util.{Failure, Success, Try}


object ActorsUtils {

  def handleDbComplete(sender: ActorRef, requestContext: RequestContext = RequestContext())(onSuccess: Any => Any): Try[Any] => Any = {
    case Success(error: Error) => sender ! ActorResponse(error, requestContext)
    case Failure(throwable) => sender ! ActorResponse(DatabaseError(throwable.getMessage), requestContext)
    case Success(result) => onSuccess(result)
  }

}
