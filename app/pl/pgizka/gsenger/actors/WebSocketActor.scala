package pl.pgizka.gsenger.actors

import akka.actor.Actor.Receive
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import pl.pgizka.gsenger.model.{Chat, UserId}
import pl.pgizka.gsenger.persistance.DatabaseSupport
import pl.pgizka.gsenger.persistance.impl.DAL

object WebSocketActor {

  def props(out: ActorRef) = Props(classOf[WebSocketActor], out)

}

class WebSocketActor (out: ActorRef, user: ActorRef, chatManager: ActorRef) extends Actor with ActorLogging {

  override def receive: Receive = ???
}
