package pl.pgizka.gsenger.startup


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

  val startupResult = boot.start(actorSystem)

  lazy val assets = new controllers.Assets(httpErrorHandler)

  lazy val userController = new UserController(
    dataAccess = boot,
    facebookService = realFacebookService,
    actorSystem = actorSystem,
    materializer = materializer,
    userManager = startupResult.userManager,
    chatManager = startupResult.chatManager)

  lazy val chatsController = new ChatController(boot, actorSystem, startupResult.chatManager)

  lazy val messageController = new MessageController(boot, actorSystem)

  lazy val router = new Routes(httpErrorHandler, userController, chatsController, messageController, assets)
}
