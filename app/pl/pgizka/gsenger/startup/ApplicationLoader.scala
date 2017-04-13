package pl.pgizka.gsenger.startup

import pl.pgizka.gsenger.controllers.chat.ChatController
import pl.pgizka.gsenger.controllers.user.UserController
import pl.pgizka.gsenger.services.facebook.realFacebookService
import play.api.ApplicationLoader.Context
import play.api.BuiltInComponentsFromContext
import router.Routes

class ApplicationLoader extends play.api.ApplicationLoader {

  def load(context: Context) = new ApplicationModule(context).application
}

class ApplicationModule(context: Context) extends BuiltInComponentsFromContext(context) {

  lazy val assets = new controllers.Assets(httpErrorHandler)

  lazy val userController = new UserController(boot, realFacebookService)

  lazy val chatsController = new ChatController(boot)

  lazy val router = new Routes(httpErrorHandler, userController, chatsController, assets)
}
