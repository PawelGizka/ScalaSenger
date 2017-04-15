package pl.pgizka.gsenger.model

import slick.lifted.MappedTo

abstract class EntityId(value: Long) extends MappedTo[Long] {
  override def toString = value.toString
}

trait Entity[I <: EntityId] {

  def id: Option[I]

}
