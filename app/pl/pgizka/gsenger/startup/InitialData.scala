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
                       participants: Seq[Participant],
                       messages: Seq[Message],
                       chatParticipantsMap: Map[ChatId, Seq[Participant]],
                       chatMessagesMap: Map[ChatId, Seq[Message]],
                       userChatParticipantMap: Map[UserId, Seq[(Chat, Participant)]],
                       userContactMap: Map[UserId, Seq[(User, Contact)]])

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

    db.run(allAction).flatMap {
      case (users, devices, contacts, chats, participants, messages) =>
        val chatParticipantsFuture = Future(chatParticipantsMap(chats, participants))
        val chatMessagesFuture = Future(chatMessagesMap(chats, messages))
        val userChatFuture = Future(userChatMap(users, participants, chats))
        val userContactFuture = Future(userContactMap(users, contacts))

        for {
          chatParticipants <- chatParticipantsFuture
          chatMessages <- chatMessagesFuture
          userChat <- userChatFuture
          userContact <- userContactFuture
        } yield InitialData(users, devices, contacts, chats, participants, messages, chatParticipants, chatMessages, userChat, userContact)
    }
  }

  private def userContactMap(users: Seq[User], contacts: Seq[Contact]): Map[UserId, Seq[(User, Contact)]] = {
    val usersContactsInitialMap: Map[UserId, Seq[(User, Contact)]] = Map(users.map(user => (user.id.get, Seq())): _*)

    val usersMap = Map(users.map(user => (user.id.get, user)): _*)

    val usersContactsMap = contacts.foldLeft(usersContactsInitialMap)((map, contact) => {
      val usersWithContacts = (usersMap(contact.userTo), contact) +: map(contact.userFrom)
      map.+((contact.userFrom, usersWithContacts))
    })

    usersContactsMap
  }

  private def userChatMap(users: Seq[User], participants: Seq[Participant], chats: Seq[Chat]): Map[UserId, Seq[(Chat, Participant)]] = {
    val userParticipantInitialMap: Map[UserId, Seq[(Chat, Participant)]] = Map(users.map(user => (user.id.get, Seq())): _*)

    val chatsMap = Map(chats.map(chat => (chat.id.get, chat)): _*)

    val userParticipantMap = participants.foldLeft(userParticipantInitialMap)((map, participant) => {
      val participants = (chatsMap(participant.chat), participant) +: map(participant.user)
      map.+((participant.user, participants))
    })

    userParticipantMap
  }

  private def chatParticipantsMap(chats: Seq[Chat], participants: Seq[Participant]): Map[ChatId, Seq[Participant]] = {
    val initialMap: Map[ChatId, Seq[Participant]] = Map(chats.map(chat => (chat.id.get, Seq())): _*)

    participants.foldLeft(initialMap)((map, participant) => {
      val chatParticipants = participant +: map(participant.chat)
      map.+((participant.chat, chatParticipants))
    })
  }

  private def chatMessagesMap(chats: Seq[Chat], messages: Seq[Message]): Map[ChatId, Seq[Message]] = {
    val initialMap: Map[ChatId, Seq[Message]] = Map(chats.map(chat => (chat.id.get, Seq())): _*)

    messages.foldLeft(initialMap)((map, participant) => {
      val chatMessages = participant +: map(participant.chat)
      map.+((participant.chat, chatMessages))
    })
  }
}


