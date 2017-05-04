package pl.pgizka.gsenger.startup

import java.util.concurrent.TimeUnit

import akka.util.Timeout


object Implicits {
  implicit val akkAskTimeout = Timeout(3, TimeUnit.MINUTES)

}
