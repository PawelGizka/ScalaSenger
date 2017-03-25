package pl.pgizka.gsenger

import pl.pgizka.gsenger.core.{ChatsController, UserController, realFacebookService}
import play.api.ApplicationLoader.Context
import play.api.{BuiltInComponents, BuiltInComponentsFromContext}
import play.api.cache.EhCacheComponents
import play.api.routing.Router
import router.Routes

class ApplicationLoader extends play.api.ApplicationLoader {

  def load(context: Context) = new ApplicationModule(context).application
}

class ApplicationModule(context: Context) extends BuiltInComponentsFromContext(context) {

  lazy val assets = new controllers.Assets(httpErrorHandler)

  lazy val userController = new UserController(boot, realFacebookService)

  lazy val chatsController = new ChatsController(boot, realFacebookService)

  lazy val router = new Routes(httpErrorHandler, userController, assets)
}
