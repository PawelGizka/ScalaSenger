package pl.pgizka.gsenger.startup

import akka.actor.{ActorSystem, Props}
import pl.pgizka.gsenger.actors.ChatActor
import pl.pgizka.gsenger.persistance.H2DBConnector
import pl.pgizka.gsenger.persistance.impl.DAL

import scala.concurrent.{Await, Awaitable}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration


object boot extends App with H2DBConnector with DAL {

  import profile.api._

  await(db.run(create()))

  val actorSystem = ActorSystem("ScalaSengerActorSystem")

  import DefaultScenario._

  val createDefaultScenarioAction = DBIO.seq(
    users ++= userTestData,
    devices ++= deviceTestData,
    contacts ++= contactTestData,
    chats ++= chatTestData,
    participants ++= participantTestData
  )

  await(db.run(createDefaultScenarioAction))

  actorSystem.actorSelection("/user/chat1")

  db.run(chats.list()).map{chats =>
    val chatActors = chats.map(chat => actorSystem.actorOf(ChatActor.props(chat.id.get, boot), ChatActor.actorName(chat.id.get)))
    chatActors foreach(actor => println(actor.path.parent.parent.name))

  }

  def await[T](awaitable: Awaitable[T]): T = {
    Await.result(awaitable, Duration.Inf)
  }

}

