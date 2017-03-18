package pl.pgizka.gsenger

import pl.pgizka.gsenger.persistance.impl.DAL
import pl.pgizka.gsenger.persistance.{DatabaseSupport, H2DBConnector}



object boot extends App with H2DBConnector with DAL {

  db.run(create())

}

