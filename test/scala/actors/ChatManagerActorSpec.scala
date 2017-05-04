package scala.actors

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.TestActorRef
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatest.time.{Minutes, Span}
import org.scalatest._
import pl.pgizka.gsenger.actors.ChatManagerActor
import pl.pgizka.gsenger.persistance.H2DBConnector
import pl.pgizka.gsenger.persistance.impl.DAL
import pl.pgizka.gsenger.startup.InitialData


class ChatManagerActorSpec extends WordSpec with MustMatchers with OptionValues with BeforeAndAfter with BeforeAndAfterAll with ScalaFutures with MockitoSugar
  with H2DBConnector with DAL {

  import profile.api._
  import pl.pgizka.gsenger.startup.DefaultScenario._

  implicit override val patienceConfig = PatienceConfig(timeout = Span(5, Minutes))

  implicit val system = ActorSystem()

  var chatManagerActor: TestActorRef[ChatManagerActor] = _

  before {
    db.run(createDefaultScenarioAction(this)).futureValue
    val initialData = InitialData.load(this).futureValue

    chatManagerActor = TestActorRef(new ChatManagerActor(this, initialData))
  }

  after {
    db.run(schema.drop).futureValue

    chatManagerActor.stop()
  }

  override protected def afterAll(): Unit = {
    system.registerOnTermination()
  }

  "createNewChat" should {
    "responses with chat created response" in {

    }
  }

}
