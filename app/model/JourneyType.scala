/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package model

import enumeratum.{ Enum, EnumEntry, PlayJsonEnum }

sealed abstract class JourneyType extends EnumEntry

case object JourneyType extends Enum[JourneyType] with PlayJsonEnum[JourneyType] {
  val values = findValues

  case object SinglePage extends JourneyType
  case object MultiPage1 extends JourneyType
}
