package pl.pgizka.gsenger.startup


import pl.pgizka.gsenger.Utils
import pl.pgizka.gsenger.actors.{ChatManagerActor, UserManagerActor}
import pl.pgizka.gsenger.controllers.chat.ChatController
import pl.pgizka.gsenger.controllers.message.MessageController
import pl.pgizka.gsenger.controllers.user.UserController
import pl.pgizka.gsenger.services.facebook.realFacebookService
import play.api.ApplicationLoader.Context
import play.api.{Application, BuiltInComponentsFromContext}
import router.Routes

class ApplicationLoader extends play.api.ApplicationLoader {

  def load(context: Context): Application = new ApplicationModule(context).application

}

class ApplicationModule(context: Context) extends BuiltInComponentsFromContext(context) {

  boot.initiateDataAccess(dataAccess)
  val initialData = Utils.await(InitialData.load(dataAccess))

  val chatManager = actorSystem.actorOf(ChatManagerActor.props(dataAccess, initialData), "chatManager")
  val userManager = actorSystem.actorOf(UserManagerActor.props(dataAccess, initialData, realFacebookService, chatManager), "userManager")

  lazy val assets = new controllers.Assets(httpErrorHandler)

  lazy val userController = new UserController(
    dataAccess = dataAccess,
    facebookService = realFacebookService,
    actorSystem = actorSystem,
    materializer = materializer,
    userManager = userManager,
    chatManager = chatManager)

  lazy val chatsController = new ChatController(dataAccess, actorSystem, chatManager)

  lazy val messageController = new MessageController(dataAccess, actorSystem)

  lazy val router = new Routes(httpErrorHandler, userController, chatsController, messageController, assets)
}
