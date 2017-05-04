package scala.data


class BasicSpecWithDefaultScenario extends BasicSpec {

  import profile.api._
  import pl.pgizka.gsenger.startup.DefaultScenario._

//  before {
//    db.run(createDefaultScenarioAction).futureValue
//  }
//
//  after {
//    db.run(schema.drop).futureValue
//  }



}
