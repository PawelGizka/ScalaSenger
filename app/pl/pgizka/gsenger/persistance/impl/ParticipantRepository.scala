package pl.pgizka.gsenger.persistance.impl

import pl.pgizka.gsenger.controllers.chat.CreateChatRequest
import pl.pgizka.gsenger.model._
import pl.pgizka.gsenger.persistance.{EntityRepository, Profile}
import slick.profile.SqlProfile.ColumnOption.Nullable

import scala.concurrent.ExecutionContext

trait ParticipantRepository extends EntityRepository {this: ChatRepository with UserRepository with MessageRepository with Profile =>
  import profile.api._

  class Participants(tag: Tag) extends Table[Participant](tag, "Participants") with EntityTable[ParticipantId, Participant] {
    def id = column[ParticipantId]("id", O.PrimaryKey, O.AutoInc)

    def userId = column[UserId]("user_id")
    def chatId = column[ChatId]("chat_id")
    def lastViewedMessageId = column[MessageId]("last_viewed_message_id", Nullable)
    def lastViewedMessageDate = column[Long]("last_viewed_message_date", Nullable)

    def chat = foreignKey("participant_chat_id_fk", chatId, chats)(_.id)
    def user = foreignKey("user_id_fk", userId, users)(_.id)
    def lastViewedMessage = foreignKey("last_viewed_message_fk", lastViewedMessageId, messages)(_.id)

    def * = (id.?, userId, chatId, lastViewedMessageId.?, lastViewedMessageDate.?) <> (Participant.tupled, Participant.unapply)
  }

  object participants extends EntityQueries[ParticipantId, Participant, Participants](new Participants(_)) {

    override def copyEntityFields(entity: Participant, id: Option[ParticipantId]): Participant = entity.copy(id = id)

    def insertFromCreateChatRequest(createChatRequest: CreateChatRequest, chat: Chat, userId: UserId): DBIO[Seq[Participant]] = {
      val participantsIds = (createChatRequest.participants :+ userId.value).distinct
      insert(participantsIds.map(participantId => new Participant(chat, UserId(participantId))))
    }

    def isUserParticipant(chatId: ChatId, userId: UserId): DBIO[Boolean] =
      findAllParticipantsQuery(chatId).filter(_.userId === userId).exists.result

    def findAllParticipants(chats: Seq[Chat])(implicit executionContext: ExecutionContext): DBIO[Seq[(Chat, Seq[Participant])]]= {
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
