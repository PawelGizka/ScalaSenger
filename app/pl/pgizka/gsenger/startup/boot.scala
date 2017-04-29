package pl.pgizka.gsenger.startup

import pl.pgizka.gsenger.actors.{ChatManagerActor, UserManagerActor}
import pl.pgizka.gsenger.persistance.H2DBConnector
import pl.pgizka.gsenger.persistance.impl.DAL
import pl.pgizka.gsenger.services.facebook.realFacebookService

import akka.actor.{ActorRef, ActorSystem, Props}

import scala.concurrent.{Await, Awaitable}
import scala.concurrent.duration.Duration

object boot extends H2DBConnector with DAL {

  case class StartupResult(chatManager: ActorRef, userManager: ActorRef)

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
    val userManager = actorSystem.actorOf(UserManagerActor.props(boot, initialData, realFacebookService))

    StartupResult(chatManager, userManager)
  }

  def await[T](awaitable: Awaitable[T]): T = {
    Await.result(awaitable, Duration.Inf)
  }


}

