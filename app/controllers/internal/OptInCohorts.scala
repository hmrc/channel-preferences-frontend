/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package controllers.internal

import connectors.{ GenericTerms, TaxCreditsTerms, TermsType }
import model.PageType
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import play.api.libs.json.JodaWrites.{ JodaDateTimeWrites => _ }
import play.api.libs.json.{ JsArray, Json, Writes }
import play.api.mvc.PathBindable
import uk.gov.hmrc.abtest.Cohort

sealed trait OptInCohort extends Cohort {
  val id: Int
  val name: String
  val terms: TermsType
  val pageType: PageType
  val majorVersion: Int
  val description: String
  val date: LocalDate // new LocalDate("2019-02-27"),

  override def toString: String = name
}

object OptInCohort {

  def fromId(id: Int): Option[OptInCohort] = OptInCohortConfigurationValues.cohorts.values.find(c => c.id == id)

  implicit val pathBinder: PathBindable[Option[OptInCohort]] = PathBindable.bindableInt.transform(
    fromId,
    _.map(_.id).getOrElse(throw new IllegalArgumentException("Cannot generate a URL for an unknown Cohort")))

  val cohortWrites = Writes[OptInCohort] { optInCohort =>
    Json.obj(
      "id"           -> optInCohort.id,
      "name"         -> optInCohort.name,
      "pageType"     -> optInCohort.pageType,
      "majorVersion" -> optInCohort.majorVersion,
      "description"  -> optInCohort.description,
      "date"         -> optInCohort.date.toString()
    )
  }
  val listCohortWrites: Writes[List[OptInCohort]] = Writes { seq =>
    JsArray(seq.map(cohortWrites.writes))
  }

}

object IPage8 extends OptInCohort {
  override val id: Int = 8
  override val name: String = "IPage8"
  override val terms: TermsType = GenericTerms
  override val pageType: PageType = PageType.IPage
  override val majorVersion: Int = 0
  override val description: String = "SOL changes to wording to improve litigation cases"
  override val date: LocalDate = new LocalDate("2020-05-12")
}

object TCPage9 extends OptInCohort {
  override val id: Int = 9
  override val name: String = "TCPage9"
  override val terms: TermsType = TaxCreditsTerms
  override val pageType: PageType = PageType.TCPage
  override val majorVersion: Int = 0
  override val description: String = ""
  override val date: LocalDate = new LocalDate("2020-05-12")
}

object CohortCurrent {
  val ipage = IPage8
  val tcpage = TCPage9
}
