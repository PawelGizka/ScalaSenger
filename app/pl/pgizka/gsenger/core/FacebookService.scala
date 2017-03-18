package pl.pgizka.gsenger.core

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import play.api.libs.json.Json
import play.api.libs.ws.WSResponse
import play.api.libs.ws.ning.NingWSClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Failure

trait FacebookService {
  def fetchFacebookUser(facebookToken: String): Future[Either[String, FbUser]]

  def fetchFacebookFriends(facebookToken: String): Future[Either[String, Seq[FbUser]]]
}

object realFacebookService extends FacebookService {

  override def fetchFacebookUser(facebookToken: String): Future[Either[String, FbUser]] =
    fetchFacebookData(facebookToken, "me").map { wsResponse => {
      if (200 <= wsResponse.status && wsResponse.status <= 299) {
        Right (Json.parse(wsResponse.body).as[FbUser](Json.format[FbUser]))
      } else {
        Left (wsResponse.body)
      }
  }}

  override def fetchFacebookFriends(facebookToken: String): Future[Either[String, Seq[FbUser]]] =
    fetchFacebookData(facebookToken, "me/friends").map { wsResponse => {
      if (200 <= wsResponse.status && wsResponse.status <= 299) {
        implicit val fbUserFormat = Json.format[FbUser]
        Right (Json.parse(wsResponse.body).as[Seq[FbUser]])
      } else {
        Left (wsResponse.body)
      }
    }}

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  def fetchFacebookData(facebookToken: String, path: String): Future[WSResponse] = {
    val wsClient = NingWSClient()
    wsClient
      .url("https://graph.facebook.com/v2.6/" + path + "?access_token=" + facebookToken)
      .get()
  }

}

