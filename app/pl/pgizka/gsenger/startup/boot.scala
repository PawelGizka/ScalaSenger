package pl.pgizka.gsenger.startup

import pl.pgizka.gsenger.persistance.H2DBConnector
import pl.pgizka.gsenger.persistance.impl.DAL



object boot extends App with H2DBConnector with DAL {

  db.run(create())

}

