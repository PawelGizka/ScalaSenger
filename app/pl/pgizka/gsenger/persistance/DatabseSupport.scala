package pl.pgizka.gsenger.persistance

import java.net.URI

import org.slf4j.LoggerFactory
import slick.backend.DatabaseConfig


import slick.driver.JdbcProfile

trait DatabaseSupport extends Profile {

  val db: JdbcProfile#Backend#Database
}

trait H2DBConnector extends DatabaseSupport {
  val driver = slick.driver.H2Driver

  import driver.api._

  val db = Database.forConfig("databaseTest")

  val profile = driver
}
