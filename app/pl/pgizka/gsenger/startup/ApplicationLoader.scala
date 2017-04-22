package pl.pgizka.gsenger.startup

import akka.actor.ActorSystem
import com.google.inject.{Inject, Singleton}
import pl.pgizka.gsenger.controllers.chat.ChatController
import pl.pgizka.gsenger.controllers.message.MessageController
import pl.pgizka.gsenger.controllers.user.UserController
import pl.pgizka.gsenger.services.facebook.realFacebookService
import pl.pgizka.gsenger.startup.boot.StartupResult
import play.api.ApplicationLoader.Context
import play.api.{Application, BuiltInComponentsFromContext}
import play.api.libs.concurrent.Akka
import router.Routes

class ApplicationLoader extends play.api.ApplicationLoader {

  def load(context: Context): Application = new ApplicationModule(context).application

}

class ApplicationModule(context: Context) extends BuiltInComponentsFromContext(context) {

  val startupResult = boot.start(actorSystem)

  lazy val assets = new controllers.Assets(httpErrorHandler)

  lazy val userController = new UserController(boot, realFacebookService, startupResult.userManager)

  lazy val chatsController = new ChatController(boot, actorSystem, startupResult.chatManager)

  lazy val messageController = new MessageController(boot, actorSystem)

  lazy val router = new Routes(httpErrorHandler, userController, chatsController, messageController, assets)
}
