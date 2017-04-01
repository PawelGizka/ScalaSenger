package pl.pgizka.gsenger.persistance.impl

import java.time.Instant

import pl.pgizka.gsenger.model._
import pl.pgizka.gsenger.persistance.{EntityRepository, Profile}
import slick.profile.SqlProfile.ColumnOption.Nullable
import scala.concurrent.ExecutionContext.Implicits.global

trait MessageRepository extends EntityRepository {this: ChatRepository with UserRepository with Profile =>

  import profile.api._

  class Messages(tag: Tag) extends Table[Message](tag, "Messages") with EntityTable[MessageId, Message] {
    def id = column[MessageId]("id", O.PrimaryKey, O.AutoInc)
    def version = column[Version]("version", Nullable)
    def created = column[Instant]("created", Nullable)
    def modified = column[Instant]("modified", Nullable)

    def chatId = column[ChatId]("chat_id")
    def senderId = column[UserId]("user_id")
    def number = column[Long]("number")
    def text = column[String]("text")

    def chat = foreignKey("chat_id_fk", chatId, chats)(_.id)
    def sender = foreignKey("sender_id_fk", senderId, users)(_.id)

    def * = (id.?, version.?, created.?, modified.?, chatId, senderId, number, text) <> ((Message.apply _).tupled, Message.unapply)
  }

  object messages extends EntityQueries[MessageId, Message, Messages](new Messages(_)) {

    override def copyEntityFields(entity: Message, id: Option[MessageId], version: Option[Version], created: Option[Instant], modified: Option[Instant]): Message =
      entity.copy(id = id, version = version, created = created, modified = modified)

    def insert(chatId: ChatId, senderId: UserId, text: String): DBIO[Message] = {
      val lastMessages: DBIO[Seq[Message]] = messages.filter(_.chatId === chatId).sortBy(_.number.desc).take(1).result

      lastMessages.flatMap{lastMessages =>
        insert(new Message(chatId, senderId, text, lastMessages.head.number + 1))
      }
    }
  }

}
