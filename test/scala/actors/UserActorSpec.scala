package scala.actors

import pl.pgizka.gsenger.actors.UserActor
import pl.pgizka.gsenger.dtos.users.GetContactsRequestDto
import pl.pgizka.gsenger.services.facebook.{FacebookService, FbUser}
import pl.pgizka.gsenger.startup.InitialData
import pl.pgizka.gsenger.startup.DefaultScenario._
import pl.pgizka.gsenger.actors.UserActor.GetContactsResponse
import pl.pgizka.gsenger.startup.Implicits.akkAskTimeout

import akka.testkit.TestActorRef
import akka.pattern.ask

import org.mockito.Mockito.when

import org.specs2.mock.mockito.MockitoMatchers.any

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global

class UserActorSpec extends ActorSpec {

  val facebookService = mock[FacebookService]

  var userActor: TestActorRef[UserActor] = _

  override def onBefore(initialData: InitialData) {

    userActor = TestActorRef(new UserActor(
      user = user1,
      dataAccess = this,
      chatsLoaded = initialData.userChatParticipantMap(user1.id.get),
      contactsLoaded = initialData.userContactMap(user1.id.get),
      facebookService = facebookService))
  }

  override def onAfter() {

    userActor.stop()
  }

  "getContacts" should {
    "return updated list of contacts when facebook service is available" in {
      val fbUsers = Seq(FbUser("facebook id 3", None, "firstName", "name", None))
      when(facebookService.fetchFacebookFriends(any[String])(any[ExecutionContext])) thenReturn Future(Right(fbUsers))

      val phoneNumbers = List(400)
      val getContactsRequestDto = GetContactsRequestDto(phoneNumbers)

      userActor ? UserActor.GetContacts(getContactsRequestDto) map {message =>
        message mustBe an[GetContactsResponse]

        val GetContactsResponse(contacts, _) = message.asInstanceOf[GetContactsResponse]

        contacts must have size 3
      }
    }
  }

}
