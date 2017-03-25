package pl.pgizka.gsenger.core

import pl.pgizka.gsenger.Utils._
import pl.pgizka.gsenger.core.errors.CouldNotFindUsersError
import pl.pgizka.gsenger.model.{Chat, User, UserId}
import pl.pgizka.gsenger.persistance.DatabaseSupport
import pl.pgizka.gsenger.persistance.impl.DAL
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Action

import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global


class ChatsController(override val dataAccess: DAL with DatabaseSupport, val facebookService: FacebookService) extends CommonController(dataAccess) {
  import dataAccess._
  import profile.api._

  def createChat: Action[JsValue] = Authenticate.async(parse.json) { request =>
    val createChatRequest = request.body.as[CreateChatRequest]

    db.run(users.find(createChatRequest.participants.map(UserId))).flatMap{foundUsers =>
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
    val errorMessage = "Not found users ids: " +
      getNotFoundElements(createChatRequest.participants, foundUsers.map(_.id.get.value)).foldLeft("")((a, b) => a + ",  " + b)
    Future(BadRequest(Json.toJson(new ErrorResponse(CouldNotFindUsersError, errorMessage))))
  }



}
