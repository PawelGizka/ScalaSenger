package pl.pgizka.gsenger.persistance.impl

import pl.pgizka.gsenger.persistance.Profile


trait DAL extends UserRepository
  with DeviceRepository
  with TokenRepository
  with ContactRepository
  with ChatRepository
  with MessageRepository
  with ParticipantRepository {this: Profile =>

  import profile.api._

  val schema = users.schema ++ devices.schema ++ participants.schema ++ tokens.schema ++
    contacts.schema ++ chats.schema ++ messages.schema

  def create() = schema.create

  def drop() = schema.drop
}

