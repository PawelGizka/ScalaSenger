package scala.actors

import akka.actor.ActorSystem
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatest.time.{Minutes, Span}
import pl.pgizka.gsenger.persistance.H2DBConnector
import pl.pgizka.gsenger.persistance.impl.DAL
import pl.pgizka.gsenger.startup.InitialData


class ActorSpec extends WordSpec with MustMatchers with OptionValues with BeforeAndAfter with BeforeAndAfterAll with ScalaFutures with MockitoSugar
  with H2DBConnector with DAL {

  import profile.api._
  import pl.pgizka.gsenger.startup.DefaultScenario._

  implicit override val patienceConfig = PatienceConfig(timeout = Span(5, Minutes))

  implicit val system = ActorSystem()

  before {
    db.run(createDefaultScenarioAction(this)).futureValue
    val initialData = InitialData.load(this).futureValue

    onBefore(initialData)
  }

  after {
    db.run(schema.drop).futureValue

    onAfter()
  }

  def onBefore(initialData: InitialData): Unit = {}

  def onAfter(): Unit = {}

  override protected def afterAll(): Unit = {
    system.registerOnTermination()
  }

}
