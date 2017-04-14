package scala.data.impl

import scala.data.{BasicSpec, BasicSpecWithDefaultScenario}

class TokenRepositorySpec extends BasicSpecWithDefaultScenario {
  import scala.concurrent.ExecutionContext.Implicits.global
  import profile.api._

  import pl.pgizka.gsenger.startup.DefaultScenario._

  "generateTokens" should {
    "generate new token for user" in {
      val token = db.run(tokens.generateToken(user1.id.get)).futureValue
      token.userId must equal(user1.id.get)
    }
  }

}
