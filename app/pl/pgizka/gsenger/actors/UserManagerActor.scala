package pl.pgizka.gsenger.actors

import akka.actor.Actor.Receive
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import pl.pgizka.gsenger.actors.UserManagerActor.UserAdded
import pl.pgizka.gsenger.model._
import pl.pgizka.gsenger.persistance.DatabaseSupport
import pl.pgizka.gsenger.persistance.impl.DAL
import pl.pgizka.gsenger.services.facebook.FacebookService
import pl.pgizka.gsenger.startup.InitialData

object UserManagerActor {

  def props(dataAccess: DAL with DatabaseSupport,
            initialData: InitialData,
            facebookService: FacebookService): Props = Props(classOf[UserManagerActor], dataAccess, initialData)

  case class UserAdded(user: User)
}

class UserManagerActor(dataAccess: DAL with DatabaseSupport,
                       initialData: InitialData,
                       facebookService: FacebookService) extends Actor with ActorLogging {

  import dataAccess._
  import profile.api._

  private implicit val executionContext = context.dispatcher

  var userActors: Map[UserId, ActorRef] = _

  @scala.throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    val userActorSeq = initialData.users.map{user =>
      val userId = user.id.get
      val userActorRef = createUserActor(user, initialData.userChatParticipantMap(userId), initialData.userContactMap(userId))
      (userId, userActorRef)
    }

    userActors = Map(userActorSeq: _*)
  }

  override def receive: Receive = {
    case UserAdded(user) =>
      val userActor = createUserActor(user, Seq(), Seq())
      userActors = userActors.+((user.id.get, userActor))
  }

  private def createUserActor(user: User, chatsWithParticipant: Seq[(Chat, Participant)], contacts: Seq[(User, Contact)]) =
    context.actorOf(UserActor.props(user, dataAccess, chatsWithParticipant, contacts, facebookService), UserActor.actorName(user.id.get))
}
