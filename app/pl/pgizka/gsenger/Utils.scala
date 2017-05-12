package pl.pgizka.gsenger

import java.time.{LocalDateTime, ZoneOffset}

import pl.pgizka.gsenger.model._
import play.api.libs.json.OFormat

import scala.collection.mutable
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Awaitable}


object Utils {

  def getNotFoundElements[A](toFind: Seq[A], found: Seq[A]): Seq[A] = {
    val map = mutable.Map[A, Boolean](toFind.map((_, true)): _*)
    found.map(map.remove)
    map.keySet.toSeq
  }

  def formatSequenceMessage[A](message: String, elements: Seq[A]): String =
    message + " " + elements.foldLeft("")((a, b) => a + ",  " + b)

  def await[T](awaitable: Awaitable[T]): T = {
    Await.result(awaitable, Duration.Inf)
  }

  /**
    * Type alias for OFormat[A]
    * @tparam A
    */
  type Js[A] = OFormat[A]

}
