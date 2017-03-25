package pl.pgizka.gsenger

import scala.collection.mutable


object Utils {

  def getNotFoundElements[A](toFind: Seq[A], found: Seq[A]): Seq[A] = {
    val map = mutable.Map[A, Boolean](toFind.map((_, true)): _*)
    found.map(map.remove)
    map.keySet.toSeq
  }

}
