package scala.data


import java.time.{Clock, LocalDateTime, ZoneOffset, ZonedDateTime}

import pl.pgizka.gsenger.model.Version
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}
import org.scalatest.{BeforeAndAfter, FunSpec, ShouldMatchers}
import pl.pgizka.gsenger.persistance.{H2DBConnector, StaleStateException}

import data._
import scala.concurrent.ExecutionContext.Implicits.global

class EntityRepositorySpec extends FunSpec with ShouldMatchers with BeforeAndAfter
  with ScalaFutures with H2DBConnector with TestEntityRepository {

  implicit override val patienceConfig = PatienceConfig(timeout = Span(5, Seconds))

  import profile.api._

  val time = LocalDateTime.of(2014, 2, 26, 9, 30)
  val inst = time.toInstant(ZoneOffset.UTC)

  val testData = List(
    TestEntity(Some(TestEntityId(1)), Some(Version(0)), Some(inst), Some(inst), "Entity 1"),
    TestEntity(Some(TestEntityId(2)), Some(Version(0)), Some(inst), Some(inst), "Entity 2")
  )

  before {
    db.run(DBIO.seq(
      testEntities.schema.create,
      testEntities ++= testData
    )).futureValue
  }

  after {
    db.run(testEntities.schema.drop).futureValue
  }

  describe("list") {
    it("should return all entities in the table") {
      db.run(testEntities.list()).futureValue should equal (testData)
    }
  }

  describe("get") {
    it("should return the entity with the given ID") {
      db.run(testEntities.get(TestEntityId(2))).futureValue should equal (testData(1))
    }

    it("should throw an exception if the given ID does not exist") {
      an[Exception] shouldBe thrownBy {
        db.run(testEntities.get(TestEntityId(3))).futureValue
      }
    }
  }

  describe("find") {
    it("should return the entity with the given ID") {
      db.run(testEntities.find(TestEntityId(2))).futureValue should equal (Some(testData(1)))
    }

    it("should return None if the given ID does not exist") {
      db.run(testEntities.find(TestEntityId(3))).futureValue should equal (None)
    }
  }

  describe("count") {
    it("should return the number of entities in the table") {
      db.run(testEntities.count()).futureValue should equal (2)
    }
  }

  describe("insert") {
    it("should insert the entity and return the entity back with ID populated") {
      val result = db.run(testEntities.insert(TestEntity(None, None, None, None, "New Entity"))).futureValue

      result.id should equal (Some(TestEntityId(3)))
      result.version should equal (Some(Version(0)))
      result.created shouldBe defined
      result.modified shouldBe defined

      db.run(testEntities.list()).futureValue should contain (result)
    }
  }

  describe("update") {
    it("should update the entity and return the entity back with the next version number") {
      val result = db.run(testEntities.update(testData.head.copy(name = "Updated name"))).futureValue

      result.version should equal (Some(Version(1)))
    }

    it("should throw an optimistic locking exception if an update on the same version number has preceeded the update") {
      val update1 = db.run(testEntities.update(testData.head.copy(name = "Update #1"))).futureValue
      update1.version should equal (Some(Version(1)))

      whenReady(db.run(testEntities.update(testData.head.copy(name = "Update #2"))).failed) { e =>
        e shouldBe an [StaleStateException]
      }

      db.run(testEntities.get(TestEntityId(1))).futureValue should equal (update1)
    }
  }

  describe("delete") {
    it("should delete the entity with the given ID and return the number of entities deleted") {
      db.run(testEntities.delete(TestEntityId(1))).futureValue should equal (1)

      db.run(testEntities.find(TestEntityId(1))).futureValue should equal (None)
    }
  }
}

