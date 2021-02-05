/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package model
import enumeratum.{ Enum, EnumEntry }
import play.api.libs.json._

sealed abstract class Language(override val entryName: String) extends EnumEntry

case object Language extends Enum[Language] {

  val values = findValues

  case object English extends Language("en")

  case object Welsh extends Language("cy")

  implicit val languageReads = Reads[Language] { js =>
    js match {
      case JsString(value) => JsSuccess(Language.withNameInsensitiveOption(value).getOrElse(Language.English))
      case _               => JsSuccess(Language.English)
    }
  }
  implicit val languageWrites = new Writes[Language] {
    override def writes(e: Language): JsValue = JsString(e.entryName)
  }
}
