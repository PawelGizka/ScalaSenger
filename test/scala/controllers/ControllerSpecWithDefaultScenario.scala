package scala.controllers

import akka.testkit.TestKitBase
import org.specs2.execute.Results

import scala.data.{BasicSpec, BasicSpecWithDefaultScenario}


abstract class ControllerSpecWithDefaultScenario extends BasicSpecWithDefaultScenario with Results with TestKitBase {

}

