package scala.data.impl

import pl.pgizka.gsenger.dtos.chats.CreateChatRequestDto

import scala.data.BasicSpecWithDefaultScenario

class ParticipantRepositorySpec extends BasicSpecWithDefaultScenario {
  import scala.concurrent.ExecutionContext.Implicits.global
  import profile.api._

  import pl.pgizka.gsenger.startup.DefaultScenario._

  "insertFromChatRequest" should {
    "create and insert all participants from charRequest" in {
      val participantIds = Seq(user2.id.get, user3.id.get)
      val createChatRequest = CreateChatRequestDto(chat1.chatType, chat1.name, participantIds)

      val result = db.run(participants.insertFromCreateChatRequest(createChatRequest, chat1, user1.id.get)).futureValue

      result must have size 3
      result.foreach{ participant =>
        participant.chat must equal(chat1.id.get)
      }
    }
  }

  "findAllParticipants" should {
    "return all participants for specified chats" in {
      val result = db.run(participants.findAllParticipants(chatTestData)).futureValue

      result must have size 2
      result foreach{case (chat, participants) =>
        participants must have size 2
      }
    }
  }


}
