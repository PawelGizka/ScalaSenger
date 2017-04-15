package scala.data

import java.time.Instant

import pl.pgizka.gsenger.model.{Entity, EntityId}
import pl.pgizka.gsenger.persistance.{EntityRepository, Profile}

case class TestEntityId(value: Long) extends EntityId(value)

case class TestEntity(id: Option[TestEntityId], name: String) extends Entity[TestEntityId]

trait TestEntityRepository extends EntityRepository { this: Profile =>

  import profile.api._

  class TestEntities(tag: Tag) extends Table[TestEntity](tag, "test_entities") with EntityTable[TestEntityId, TestEntity] {
    def id = column[TestEntityId]("id", O.PrimaryKey, O.AutoInc)

    def name = column[String]("name")

    def * = (id.?, name) <> (TestEntity.tupled, TestEntity.unapply)
  }

  object testEntities extends EntityQueries[TestEntityId, TestEntity, TestEntities](new TestEntities(_)) {

    override def copyEntityFields(entity: TestEntity, id: Option[TestEntityId]): TestEntity = entity.copy(id = id)
  }
}

