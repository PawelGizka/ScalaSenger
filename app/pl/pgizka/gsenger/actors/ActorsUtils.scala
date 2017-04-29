package pl.pgizka.gsenger.actors

import akka.actor.ActorRef
import pl.pgizka.gsenger.Error
import pl.pgizka.gsenger.errors.DatabaseError

import scala.util.{Failure, Success, Try}


object ActorsUtils {

  def handleDbComplete(sender: ActorRef)(onSuccess: Any => Any): Try[Any] => Any = {
    case Success(error: Error) => sender ! error
    case Failure(throwable) => sender ! DatabaseError(throwable.getMessage)
    case Success(result) => onSuccess(result)
  }

}
