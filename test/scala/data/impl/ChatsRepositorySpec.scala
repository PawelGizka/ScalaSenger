package scala.data.impl

import scala.data.{BasicSpecWithDefaultScenario}
import scala.concurrent.ExecutionContext.Implicits.global

class ChatsRepositorySpec extends BasicSpecWithDefaultScenario {
  import profile.api._

  import scala.data.DefaultScenario._

  "findAllChats" should {
    "return all chats for specified user" in {
      val foundChats = db.run(chats.findAllChats(user1.id.get)).futureValue

      foundChats must have size 2
    }
  }


}
