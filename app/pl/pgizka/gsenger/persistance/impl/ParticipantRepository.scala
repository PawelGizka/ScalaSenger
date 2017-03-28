package pl.pgizka.gsenger.persistance.impl

import java.time.Instant

import pl.pgizka.gsenger.core.CreateChatRequest
import pl.pgizka.gsenger.model._
import pl.pgizka.gsenger.persistance.{EntityRepository, Profile}
import slick.profile.SqlProfile.ColumnOption.Nullable
import scala.concurrent.ExecutionContext.Implicits.global


trait ParticipantRepository extends EntityRepository {this: ChatRepository with UserRepository with MessageRepository with Profile =>
  import profile.api._

  class Participants(tag: Tag) extends Table[Participant](tag, "Participants") with EntityTable[ParticipantId, Participant] {
    def id = column[ParticipantId]("id", O.PrimaryKey, O.AutoInc)
    def version = column[Version]("version", Nullable)
    def created = column[Instant]("created", Nullable)
    def modified = column[Instant]("modified", Nullable)

    def userId = column[UserId]("user_id")
    def chatId = column[ChatId]("chat_id")
    def lastViewedMessageId = column[MessageId]("last_viewed_message_id", Nullable)
    def lastViewedMessageDate = column[Long]("last_viewed_message_date", Nullable)

    def chat = foreignKey("participant_chat_id_fk", chatId, chats)(_.id)
    def user = foreignKey("user_id_fk", userId, users)(_.id)
    def lastViewedMessage = foreignKey("last_viewed_message_fk", lastViewedMessageId, messages)(_.id)

    def * = (id.?, version.?, created.?, modified.?, userId, chatId, lastViewedMessageId.?, lastViewedMessageDate.?) <> (Participant.tupled, Participant.unapply)
  }

  object participants extends EntityQueries[ParticipantId, Participant, Participants](new Participants(_)) {

    override def copyEntityFields(entity: Participant, id: Option[ParticipantId], version: Option[Version], created: Option[Instant], modified: Option[Instant]): Participant =
      entity.copy(id = id, version = version, created = created, modified = modified)

    def insertFromCreateChatRequest(createChatRequest: CreateChatRequest, chat: Chat, user: User): DBIO[Seq[Participant]] = {
      val participantsIds = (createChatRequest.participants :+ user.idValue).distinct
      insert(participantsIds.map(participantId => new Participant(chat, UserId(participantId))))
    }

    def findAllParticipants(chats: Seq[Chat]): DBIO[Seq[(Chat, Seq[Participant])]]= {
      DBIO.sequence(chats.map(chat => findAllParticipants(chat.id.get).map(participantsFound =>(chat, participantsFound))))
    }

    def findAllParticipants(chatId: ChatId): DBIO[Seq[Participant]] = findAllParticipantsQuery(chatId).result

    def findAllParticipantsQuery(chatId: ChatId): Query[Participants, Participant, Seq] = {
      for {
        participant <- participants
        if participant.chatId === chatId
      } yield participant
    }
  }

}
