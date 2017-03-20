package pl.pgizka.gsenger.persistance.impl

import java.time.Instant

import pl.pgizka.gsenger.model._
import pl.pgizka.gsenger.persistance.{EntityRepository, Profile}
import slick.profile.SqlProfile.ColumnOption.Nullable


trait ChatRepository extends EntityRepository {this: Profile =>

  import profile.api._

  class Chats(tag: Tag) extends Table[Chat](tag, "Chats") with EntityTable[ChatId, Chat] {
    def id = column[ChatId]("id", O.PrimaryKey, O.AutoInc)
    def version = column[Version]("version", Nullable)
    def created = column[Instant]("created", Nullable)
    def modified = column[Instant]("modified", Nullable)

    def chatType = column[String]("type")
    def name = column[String]("name", Nullable)
    def started = column[Instant]("started")

    def * = (id.?, version.?, created.?, modified.?, chatType, name.?, started) <> (Chat.tupled, Chat.unapply)
  }

  object chats extends EntityQueries[ChatId, Chat, Chats](new Chats(_)) {

    override def copyEntityFields(entity: Chat, id: Option[ChatId], version: Option[Version], created: Option[Instant], modified: Option[Instant]): Chat =
      entity.copy(id = id, version = version, created = created, modified = modified)


  }

}
