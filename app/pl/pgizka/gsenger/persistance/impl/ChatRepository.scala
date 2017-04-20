package pl.pgizka.gsenger.persistance.impl

import java.time.Instant

import pl.pgizka.gsenger.controllers.chat.CreateChatRequest
import pl.pgizka.gsenger.model._
import pl.pgizka.gsenger.persistance.{EntityRepository, Profile}
import slick.profile.SqlProfile.ColumnOption.Nullable

import scala.concurrent.ExecutionContext

trait ChatRepository extends EntityRepository {this: ParticipantRepository with ContactRepository with Profile =>

  import profile.api._

  class Chats(tag: Tag) extends Table[Chat](tag, "Chats") with EntityTable[ChatId, Chat] {
    def id = column[ChatId]("id", O.PrimaryKey, O.AutoInc)

    def chatType = column[String]("type")
    def name = column[String]("name", Nullable)
    def started = column[Instant]("started")

    def * = (id.?, chatType, name.?, started) <> (Chat.tupled, Chat.unapply)
  }

  object chats extends EntityQueries[ChatId, Chat, Chats](new Chats(_)) {

    override def copyEntityFields(entity: Chat, id: Option[ChatId]): Chat = entity.copy(id = id)

    def findAllChats(userId: UserId)(implicit executionContext: ExecutionContext): DBIO[Seq[Chat]] = {
      (for {
        participant <- participants
        if participant.userId === userId
        chat <- participant.chat
      } yield chat).result
    }

    def insertFromRequest(createChatRequest: CreateChatRequest, user: User): DBIO[Chat] = (for {
      chat <- chats.insert(new Chat(createChatRequest))
      participants <- participants.insertFromCreateChatRequest(createChatRequest, chat, user)
      _ <- contacts.ensureEverybodyKnowsEachOther(participants)
    } yield chat).transactionally

  }
}
