package scala.data

import org.scalatest.time.{Seconds, Span}
import pl.pgizka.gsenger.startup.InitialData

import scala.concurrent.ExecutionContext.Implicits.global

class EntityRepositorySpec extends BasicSpec with TestEntityRepository {

  implicit override val patienceConfig = PatienceConfig(timeout = Span(5, Seconds))

  import profile.api._

  val testData = List(
  TestEntity(Some(TestEntityId(1)), "Entity 1"),
  TestEntity(Some(TestEntityId(2)), "Entity 2")
  )

  override def onBefore(initialData: InitialData): Unit = {
    db.run(DBIO.seq(
    testEntities.schema.create,
    testEntities ++= testData
    )).futureValue
  }

  override def onAfter(): Unit = {
    db.run(testEntities.schema.drop).futureValue
  }

  "list" should {
    "return all entities in the table" in {
      db.run(testEntities.list()).futureValue must equal(testData)
    }
  }

  "get" should {
    "return the entity with the given ID" in {
      db.run(testEntities.get(TestEntityId(2))).futureValue must equal (testData(1))
    }

    "throw an exception if the given ID does not exist" in {
      an[Exception] shouldBe thrownBy {
        db.run(testEntities.get(TestEntityId(3))).futureValue
      }
    }
  }

  "find" should {
    "return the entity with the given ID" in {
      db.run(testEntities.find(TestEntityId(2))).futureValue must equal (Some(testData(1)))
    }

    "return None if the given ID does not exist" in {
      db.run(testEntities.find(TestEntityId(3))).futureValue must equal (None)
    }
  }

  "count" should {
    "return the number of entities in the table" in {
      db.run(testEntities.count()).futureValue must equal (2)
    }
  }

  "insert" should {
    "insert the entity and return the entity back with ID populated" in {
      val result = db.run(testEntities.insert(TestEntity(None, "New Entity"))).futureValue

      result.id must equal (Some(TestEntityId(3)))

      db.run(testEntities.list()).futureValue must contain (result)
    }
  }

  "update" should {
    "update the entity" in {
      val updated = testData.head.copy(name = "Updated name")
      db.run(testEntities.update(updated)).futureValue

      val result = db.run(testEntities.find(testData.head.id.get)).futureValue

      result.get must equal(updated)
    }
  }

  "delete" should {
    "delete the entity with the given ID and return the number of entities deleted" in {
      db.run(testEntities.delete(TestEntityId(1))).futureValue must equal (1)

      db.run(testEntities.find(TestEntityId(1))).futureValue must equal (None)
    }
  }

}

