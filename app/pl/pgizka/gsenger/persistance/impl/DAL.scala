package pl.pgizka.gsenger.persistance.impl

import pl.pgizka.gsenger.persistance.Profile

/**
  * A cake of all our data repositories that allows us to build a schema of our entire database.
  * Useful for the application as it exposes all available queries, not so much for testing of individual components.
  */
trait DAL extends UserRepository with DeviceRepository with TokenRepository with ContactsRepository {this: Profile =>
  import profile.api._

  val schema = users.schema ++ devices.schema ++ tokens.schema ++ contacts.schema

  def create() = schema.create

  def drop() = schema.drop
}

