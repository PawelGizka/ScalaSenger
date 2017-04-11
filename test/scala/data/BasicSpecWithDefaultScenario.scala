package scala.data


class BasicSpecWithDefaultScenario extends BasicSpec {

  import profile.api._
  import DefaultScenario._

  before {
    db.run(createDefaultScenarioAction).futureValue
  }

  after {
    db.run(schema.drop).futureValue
  }

  val createDefaultScenarioAction = DBIO.seq(
    schema.create,
    users ++= userTestData,
    devices ++= deviceTestData,
    contacts ++= contactTestData,
    chats ++= chatTestData,
    participants ++= participantTestData
  )

}
