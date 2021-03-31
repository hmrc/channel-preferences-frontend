/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package model

import enumeratum.{ Enum, EnumEntry, PlayJsonEnum }
import org.joda.time.DateTime
import play.api.libs.json.{ Format, Json, Reads, Writes }
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats

sealed trait SurveyType extends EnumEntry

object SurveyType extends Enum[SurveyType] with PlayJsonEnum[SurveyType] {

  val values = findValues

  case object StandardInterruptOptOut extends SurveyType

}

final case class Survey(surveyType: SurveyType, completedAt: DateTime)

object Survey {
  implicit val dateReads: Reads[DateTime] = ReactiveMongoFormats.dateTimeRead
  implicit val dateWrites: Writes[DateTime] = ReactiveMongoFormats.dateTimeWrite
  implicit val suveyFormat: Format[Survey] = Json.format
}
