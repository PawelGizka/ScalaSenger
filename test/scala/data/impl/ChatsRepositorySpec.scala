package scala.data.impl

import scala.data.{BasicSpec}
import scala.concurrent.ExecutionContext.Implicits.global

class ChatsRepositorySpec extends BasicSpec {
  import profile.api._

  import pl.pgizka.gsenger.startup.DefaultScenario._

  "findAllChats" should {
    "return all chats for specified user" in {
      val foundChats = db.run(chats.findAllChats(user1.id.get)).futureValue

      foundChats must have size 2
    }
  }


}
