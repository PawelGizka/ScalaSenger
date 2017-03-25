package scala.data

import java.time.{LocalDateTime, ZoneOffset}

import org.scalatest.BeforeAndAfter
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatest.time.{Minutes, Span}
import org.scalatestplus.play.PlaySpec
import pl.pgizka.gsenger.persistance.H2DBConnector
import pl.pgizka.gsenger.persistance.impl.DAL


class RepositorySpec extends PlaySpec with BeforeAndAfter with ScalaFutures with MockitoSugar
  with H2DBConnector with DAL {

  implicit override val patienceConfig = PatienceConfig(timeout = Span(5, Minutes))
  val time = LocalDateTime.of(2014, 2, 26, 9, 30)
  val inst = time.toInstant(ZoneOffset.UTC)


}
