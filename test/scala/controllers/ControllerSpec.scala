package scala.controllers

import akka.actor.ActorSystem
import akka.testkit.TestKitBase
import org.specs2.execute.Results

import scala.data.BasicSpec


abstract class ControllerSpec extends BasicSpec with Results with TestKitBase {
  implicit lazy val system: ActorSystem = ActorSystem()

}

