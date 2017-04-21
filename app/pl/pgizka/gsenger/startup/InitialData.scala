package pl.pgizka.gsenger.startup


import pl.pgizka.gsenger.model._
import pl.pgizka.gsenger.persistance.DatabaseSupport
import pl.pgizka.gsenger.persistance.impl.DAL
import slick.dbio.DBIO

import scala.concurrent.Future


case class InitialData(users: Seq[User],
                       devices: Seq[Device],
                       contacts: Seq[Contact],
                       chats: Seq[Chat],
                       participants: Map[ChatId, Seq[Participant]],
                       messages: Map[ChatId, Seq[Message]])

object InitialData {
  def load(dataAccess: DAL with DatabaseSupport): Future[InitialData] = {
    import dataAccess._
    import profile.api

    val allAction = for {
      users <- users.list()
      devices <- devices.list()
      contacts <- contacts.list()
      chats <- chats.list()
      participants <- participants.list()
      messages <- messages.list()
    } yield (users, devices, contacts, chats, participants, messages)

    db.run(allAction).map{
      case (users, devices, contacts, chats, participants, messages) =>
        InitialData(users, devices, contacts, chats,
          participantsMap(chats, participants), messagesMap(chats, messages))
    }
  }

  private def participantsMap(chats: Seq[Chat], participants: Seq[Participant]): Map[ChatId, Seq[Participant]] = {
    val initialMap: Map[ChatId, Seq[Participant]] = Map(chats.map(chat => (chat.id.get, Seq())): _*)

    participants.foldLeft(initialMap)((map, participant) => {
      val chatParticipants = participant +: map(participant.chat)
      map.+((participant.chat, chatParticipants))
    })
  }

  private def messagesMap(chats: Seq[Chat], messages: Seq[Message]): Map[ChatId, Seq[Message]] = {
    val initialMap: Map[ChatId, Seq[Message]] = Map(chats.map(chat => (chat.id.get, Seq())): _*)

    messages.foldLeft(initialMap)((map, participant) => {
      val chatMessages = participant +: map(participant.chat)
      map.+((participant.chat, chatMessages))
    })
  }
}


