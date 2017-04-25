package pl.pgizka.gsenger.actors

import akka.actor.Actor.Receive
import akka.actor.{Actor, ActorLogging, ActorRef, Identify, Props}
import pl.pgizka.gsenger.actors.UserActor.{NewWebSocketConnection, WebSocketConnectionClosed}
import pl.pgizka.gsenger.model.{Chat, UserId}
import pl.pgizka.gsenger.persistance.DatabaseSupport
import pl.pgizka.gsenger.persistance.impl.DAL

object WebSocketActor {

  def props(out: ActorRef, userId: UserId, userActor: ActorRef, chatManagerActor: ActorRef) =
    Props(classOf[WebSocketActor], out, userId, userActor, chatManagerActor)

}

class WebSocketActor (out: ActorRef, userId: UserId, userActor: ActorRef, chatManager: ActorRef) extends Actor with ActorLogging {

  @scala.throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    userActor ! NewWebSocketConnection(self)
  }

  override def receive: Receive = ???

  @scala.throws[Exception](classOf[Exception])
  override def postStop(): Unit = {
    userActor ! WebSocketConnectionClosed(self)
  }
}
