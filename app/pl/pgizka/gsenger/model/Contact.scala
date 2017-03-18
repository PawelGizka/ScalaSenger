package pl.pgizka.gsenger.model


case class Contact(userFrom: UserId, userTo: UserId, fromFacebook: Boolean, fromPhone: Boolean)

case class ContactUserWrapper(contact: User, fromFacebook: Boolean, fromPhone: Boolean)
