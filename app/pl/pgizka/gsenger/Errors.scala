package pl.pgizka.gsenger


case class Error(code: Int, message: String, info: Option[String] = None) {

  def apply(additionalInfo: String) = Error(this.code, this.message, Some(additionalInfo))
}

object errors {
  object UserAlreadyExistsError extends Error(1, "User already exists")
  object CouldNotFindUsersError extends Error(4, "Could not find all users by specified ids")
  object CouldNotFindChatError extends Error(5, "Could not find chat with specified id")
  object UserAccessForbidden extends Error(6, "There is no user with such access token")
  object ChatAccessForbidden extends Error(7, "You are not participant of specified chat")
  object Forbidden extends Error(9, "Access to specified resource is forbidden")

  object DatabaseError extends Error(2, "Database error has occurred")
  object FetchFacebookDataError extends Error(3, "Cannot fetch facebook data")
  object ActorAskTimeout extends Error(8, "Timeout has occurred while waiting for actor response")
}
