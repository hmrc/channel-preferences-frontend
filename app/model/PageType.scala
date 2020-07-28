/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package model
import enumeratum.{ Enum, EnumEntry, PlayJsonEnum }
import play.api.libs.json._

sealed abstract class PageType() extends EnumEntry

case object PageType extends Enum[PageType] with PlayJsonEnum[PageType] {

  val values = findValues

  case object IPage extends PageType

  case object TCPage extends PageType

  case object ReOptInPage extends PageType

}
