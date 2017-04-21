package pl.pgizka.gsenger.actors

import akka.actor.Actor.Receive
import akka.actor.{Actor, ActorLogging}
import pl.pgizka.gsenger.persistance.DatabaseSupport
import pl.pgizka.gsenger.persistance.impl.DAL

object UserManagerActor {

}

class UserManagerActor(dataAccess: DAL with DatabaseSupport) extends Actor with ActorLogging {

  import dataAccess._
  import profile.api._

  private implicit val executionContext = context.dispatcher

  override def receive: Receive = ???
}
