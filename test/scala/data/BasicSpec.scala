package scala.data

import java.time.{LocalDateTime, ZoneOffset}

import org.scalatest.BeforeAndAfter
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatest.time.{Minutes, Span}
import org.scalatestplus.play.PlaySpec
import pl.pgizka.gsenger.persistance.H2DBConnector
import pl.pgizka.gsenger.persistance.impl.DAL
import pl.pgizka.gsenger.startup.InitialData


class BasicSpec extends PlaySpec with BeforeAndAfter with ScalaFutures with MockitoSugar
  with H2DBConnector with DAL {

  implicit override val patienceConfig = PatienceConfig(timeout = Span(5, Minutes))
  val time = LocalDateTime.of(2014, 2, 26, 9, 30)
  val inst = time.toInstant(ZoneOffset.UTC)

  import profile.api._
  import pl.pgizka.gsenger.startup.DefaultScenario._

  before {
    db.run(createDefaultScenarioAction(this)).futureValue
    val initialData = InitialData.load(this).futureValue

    onBefore(initialData)
  }

  after {
    db.run(schema.drop).futureValue

    onAfter()
  }

  def onBefore(initialData: InitialData): Unit = {

  }

  def onAfter(): Unit = {}

}
