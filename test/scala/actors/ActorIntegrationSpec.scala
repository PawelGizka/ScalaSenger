package scala.actors

import java.util.concurrent.TimeUnit

import akka.actor.ActorRef
import akka.testkit.{TestKitBase, TestProbe}
import pl.pgizka.gsenger.actors.{ChatManagerActor, UserActor, UserManagerActor, WebSocketActor}
import pl.pgizka.gsenger.services.facebook.realFacebookService
import pl.pgizka.gsenger.startup.DefaultScenario.{user1, user2, user3}
import pl.pgizka.gsenger.startup.InitialData
import pl.pgizka.gsenger.startup.Implicits.akkAskTimeout

import scala.concurrent.duration.FiniteDuration

class ActorIntegrationSpec extends ActorSpec with TestKitBase {

  var chatManager: ActorRef = _
  var userManager: ActorRef = _

  var out1: TestProbe = _
  var out2: TestProbe = _
  var out3: TestProbe = _

  var webSocketActor1: ActorRef = _
  var webSocketActor2: ActorRef = _
  var webSocketActor3: ActorRef = _

  override def onBefore(initialData: InitialData): Unit = {
    chatManager = system.actorOf(ChatManagerActor.props(this, initialData), "chatManager")
    userManager = system.actorOf(UserManagerActor.props(this, initialData, realFacebookService), "userManager")
    Thread.sleep(500)

    val user1Actor = UserActor.actorSelection(user1.id.get).resolveOne().futureValue
    val user2Actor = UserActor.actorSelection(user2.id.get).resolveOne().futureValue
    val user3Actor = UserActor.actorSelection(user3.id.get).resolveOne().futureValue

    out1 = TestProbe("out1")
    out2 = TestProbe("out1")
    out3 = TestProbe("out1")

    webSocketActor1 = system.actorOf(WebSocketActor.props(out1.testActor, user1.id.get, user1Actor, chatManager, this))
    webSocketActor2 = system.actorOf(WebSocketActor.props(out2.testActor, user2.id.get, user2Actor, chatManager, this))
    webSocketActor3 = system.actorOf(WebSocketActor.props(out3.testActor, user3.id.get, user3Actor, chatManager, this))
  }

  override def onAfter(): Unit = {
    system.stop(chatManager)
    system.stop(userManager)
  }

  def receiveMessage(testProbe: TestProbe): AnyRef = testProbe.receiveOne(FiniteDuration(3, TimeUnit.MINUTES))

}
