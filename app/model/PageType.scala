/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package model
import enumeratum.{ Enum, EnumEntry }
import play.api.libs.json._

sealed abstract class PageType(override val entryName: String) extends EnumEntry

case object PageType extends Enum[PageType] {

  val values = findValues

  case object IPage extends PageType("IPage")

  case object TCPage extends PageType("TCPage")

  implicit val languageReads = Reads[PageType] { js =>
    js match {
      case JsString(value) => JsSuccess(PageType.withNameInsensitiveOption(value).get)
    }
  }
  implicit val languageWrites = new Writes[PageType] {
    override def writes(e: PageType): JsValue = JsString(e.entryName)
  }
}
