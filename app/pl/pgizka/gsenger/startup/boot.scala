package pl.pgizka.gsenger.startup

import pl.pgizka.gsenger.persistance.{DatabaseSupport, H2DBConnector}
import pl.pgizka.gsenger.persistance.impl.DAL
import pl.pgizka.gsenger.Utils.await

object dataAccess extends H2DBConnector with DAL

object boot {

  def initiateDataAccess(dataAccess: DAL with DatabaseSupport): Unit = {
    import dataAccess._
    import DefaultScenario._

    await(db.run(createDefaultScenarioAction(dataAccess)))
  }
}

