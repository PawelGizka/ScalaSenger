package scala.data.impl

import scala.data.BasicSpecWithDefaultScenario
import scala.concurrent.ExecutionContext.Implicits.global

class MessageRepositorySpec extends BasicSpecWithDefaultScenario {

  import profile.api._
  import pl.pgizka.gsenger.startup.DefaultScenario._

  "insert" should {
    "insert new message and increment last message id" in {
      val messageText = "some message text"

      val message = db.run(messages.insert(chat1.id.get, user1.id.get, messageText)).futureValue

      message.text must equal(messageText)
      message.number must equal(4)
    }
  }

}
