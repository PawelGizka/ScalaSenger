package scala.actors

import pl.pgizka.gsenger.actors.{ChatManagerActor, UserManagerActor}
import pl.pgizka.gsenger.services.facebook.realFacebookService
import pl.pgizka.gsenger.startup.InitialData

import akka.actor.ActorRef
import akka.testkit.TestKitBase

class MessageSendingIntegrationSpec extends ActorSpec with TestKitBase {

  var chatManager: ActorRef = _
  var userManager: ActorRef = _

  override def onBefore(initialData: InitialData): Unit = {
    chatManager = system.actorOf(ChatManagerActor.props(this, initialData), "chatManager")
    userManager = system.actorOf(UserManagerActor.props(this, initialData, realFacebookService), "userManager")


  }

  override def onAfter(): Unit = {
    system.stop(chatManager)
    system.stop(userManager)
  }

  "create new message" should {

    "create new message," +
      "returned it to sender (WebSocketActor," +
      "send newly created message to all participants in chat and all participants devices (WebSocketActors)" in {

    }

    "return forbidden response when user is not a member of chat" in {

    }
  }

}
