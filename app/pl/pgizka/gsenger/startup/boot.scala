package pl.pgizka.gsenger.startup

import pl.pgizka.gsenger.persistance.{DatabaseSupport, H2DBConnector}
import pl.pgizka.gsenger.persistance.impl.DAL
import pl.pgizka.gsenger.Utils.await

object dataAccess extends H2DBConnector with DAL

object boot {

  def initiateDataAccess(dataAccess: DAL with DatabaseSupport): Unit = {
    import dataAccess._
    import profile.api._

    await(db.run(create()))

    import DefaultScenario._

    val createDefaultScenarioAction = DBIO.seq(
      users ++= userTestData,
      devices ++= deviceTestData,
      contacts ++= contactTestData,
      chats ++= chatTestData,
      participants ++= participantTestData,
      messages ++= messageTestData
    )

    await(db.run(createDefaultScenarioAction))
  }
}

