package models.paye

object EngineCapacity {

  val NOT_APPLICABLE = "none"
  val engineCapacityOptions = Seq(NOT_APPLICABLE, "1400", "2000", "9999")

  def engineCapacityEmpty(valueOpt: Option[String]): Boolean = valueOpt.isEmpty || valueOpt.exists(engineCapacityEmpty)

  def engineCapacityEmpty(value: String): Boolean = value == EngineCapacity.NOT_APPLICABLE

  def mapEngineCapacityToInt(valueOption: Option[String]): Option[Int] =
    valueOption match {
      case Some(value) if !engineCapacityEmpty(value) => Some(value.toInt)
      case _ => None
    }
}
