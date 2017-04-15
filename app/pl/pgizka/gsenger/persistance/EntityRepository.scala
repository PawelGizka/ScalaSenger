package pl.pgizka.gsenger.persistance

import java.sql.Timestamp
import java.time.Instant

import pl.pgizka.gsenger.model.{Entity, EntityId, UserId}

import scala.concurrent.ExecutionContext


trait EntityRepository { this: Profile =>

  import profile.api._

  implicit val instantMapper = MappedColumnType.base[Instant, Timestamp] (
    inst => if (inst == null) null else Timestamp.from(inst),
    ts => if (ts == null) null else ts.toInstant
  )

  trait EntityTable[I <: EntityId, E <: Entity[I]] extends Table[E] {
    def id: Rep[I]
  }

  abstract class EntityQueries[I <: EntityId: BaseColumnType, E <: Entity[I], T <: EntityTable[I, E]](cons: Tag => T)
    extends TableQuery(cons) {

    def tableQuery = this

    /**
      * Subclasses simply need to copy the given ID into the entity.
      * This is because we can't access the case-class' copy method here.
      */
    def copyEntityFields(entity: E, id: Option[I]): E

    def list(): DBIO[Seq[E]] = tableQuery.result

    def get(id: I): DBIO[E] = (for (e <- tableQuery if e.id === id) yield e).result.head

    def find(id: I): DBIO[Option[E]] = (for (e <- tableQuery if e.id === id) yield e).result.headOption

    def find(ids: Seq[I]): DBIO[Seq[E]] = (for (e <- tableQuery if e.id inSetBind ids) yield e).result

    def findByIds(ids: Seq[I]): DBIO[Seq[E]] = tableQuery.filter(_.id inSetBind ids).result

    def count(): DBIO[Int] = (for (e <- tableQuery) yield e).length.result

    def insert(e: E): DBIO[E] = {

      (tableQuery returning tableQuery.map(_.id) into {
        case(t, id) => copyEntityFields(t, Some(id))
      }) += e
    }

    /**
      * Insert the given sequence of entities and return a copy of that entities with IDs, versions, createds and modifieds fields populated.
      */
    def insert(e: Seq[E]): DBIO[Seq[E]] = {

      (tableQuery returning tableQuery.map(_.id) into {
        case(t, id) => t
      }) ++= e
    }

    /**
      * Update with optimistic locking. Only updates if ID and version match up.
      * @throws StaleStateException if no matching row with ID/version.
      */
    def update(e: E)(implicit ec: ExecutionContext): DBIO[E] = {
      require(e.id.isDefined)

      val q = for (r <- tableQuery if r.id === e.id) yield r
      q.update(e).map{result =>
        if (result == 1) e else throw new StaleStateException("Updated " + result + " rows, expecting 1.")
      }
    }
    def delete(id: I): DBIO[Int] = (for (e <- tableQuery if e.id === id) yield e).delete

  }
}

