package pl.pgizka.gsenger.controllers.chat

import pl.pgizka.gsenger.Utils._
import pl.pgizka.gsenger.controllers.{CommonController, ErrorResponse}
import pl.pgizka.gsenger.core._
import pl.pgizka.gsenger.controllers.errors.CouldNotFindUsersError
import pl.pgizka.gsenger.model.{Chat, User, UserId}
import pl.pgizka.gsenger.persistance.DatabaseSupport
import pl.pgizka.gsenger.persistance.impl.DAL
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Action

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class ChatController(override val dataAccess: DAL with DatabaseSupport) extends CommonController(dataAccess) {
  import dataAccess._
  import profile.api._

  def createChat: Action[JsValue] = Authenticate.async(parse.json) { request =>
    val createChatRequest = request.body.as[CreateChatRequest]

    db.run(users.find(createChatRequest.participants.map(new UserId(_)))).flatMap{foundUsers =>
      if (foundUsers.size == createChatRequest.participants.size) {
        insertChatOkResponse(createChatRequest, request.user)
      } else {
        insertChatErrorResponse(createChatRequest, foundUsers)
      }
    } recover databaseError
  }

  private def insertChatOkResponse(createChatRequest: CreateChatRequest, user: User) = {
    db.run(insertChatDbAction(createChatRequest, user).transactionally).map{chatId =>
      Ok(Json.toJson(CreateChatResponse(chatId.value)))
    } recover databaseError
  }

  private def insertChatDbAction(createChatRequest: CreateChatRequest, user: User) = for {
    chat <- chats.insert(new Chat(createChatRequest))
    participants <- participants.insertFromCreateChatRequest(createChatRequest, chat, user)
    _ <- contacts.ensureEverybodyKnowsEachOther(participants)
  } yield chat.id.get

  private def insertChatErrorResponse(createChatRequest: CreateChatRequest, foundUsers: Seq[User]) = {
    val notFoundIds = getNotFoundElements(createChatRequest.participants, foundUsers.map(_.id.get.value))
    val errorMessage = formatSequenceMessage("Not found users ids: ", notFoundIds)
    Future(BadRequest(Json.toJson(new ErrorResponse(CouldNotFindUsersError, errorMessage))))
  }

  def listAllChatsWithParticipantInfo = Authenticate.async{ request =>
    val chatsInfo= for {
      chatsFound <- db.run(chats.findAllChats(request.user.id.get))
      chatsWithParticipants <- db.run(participants.findAllParticipants(chatsFound))
    } yield chatsWithParticipants.map(ChatInfo(_))

    chatsInfo.map{chatsInfos =>
      Ok(Json.toJson(ListAllChatsWithParticipantInfoResponse(chatsInfos)))
    } recover databaseError
  }

}
