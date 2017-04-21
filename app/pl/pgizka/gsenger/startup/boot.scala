package pl.pgizka.gsenger.startup

import akka.actor.{ActorRef, ActorSystem, Props}
import pl.pgizka.gsenger.actors.{ChatActor, ChatManagerActor}
import pl.pgizka.gsenger.model.ChatId
import pl.pgizka.gsenger.persistance.H2DBConnector
import pl.pgizka.gsenger.persistance.impl.DAL

import scala.concurrent.{Await, Awaitable}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

object boot extends H2DBConnector with DAL {

  case class StartupResult(chatManager: ActorRef)

  import profile.api._
  await(db.run(create()))

  def start(actorSystem: ActorSystem): StartupResult = {
    println("hello from start")

    import DefaultScenario._

    val createDefaultScenarioAction = DBIO.seq(
      users ++= userTestData,
      devices ++= deviceTestData,
      contacts ++= contactTestData,
      chats ++= chatTestData,
      participants ++= participantTestData,
      messages ++= messageTestData
    )

    await(db.run(createDefaultScenarioAction))

    val initialData = await(InitialData.load(this))

    val chatManager = actorSystem.actorOf(ChatManagerActor.props(boot, initialData))

    StartupResult(chatManager)
  }

  def await[T](awaitable: Awaitable[T]): T = {
    Await.result(awaitable, Duration.Inf)
  }


}

