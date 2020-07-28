/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package controllers.internal

import connectors.{ GenericTerms, TaxCreditsTerms, TermsType }
import model.PageType
import org.joda.time.LocalDate
import play.api.libs.json.JodaWrites.{ JodaDateTimeWrites => _ }
import play.api.libs.json._
import play.api.mvc.PathBindable
import uk.gov.hmrc.abtest.Cohort

import scala.util.Try

sealed trait OptInCohort extends Cohort {
  val id: Int
  val name: String
  val terms: TermsType
  val pageType: PageType
  val majorVersion: Int
  val minorVersion: Int
  val description: String
  val date: LocalDate // new LocalDate("2019-02-27"),

  override def toString: String = name
}

object OptInCohort {

  def fromId(id: Int): Option[OptInCohort] = OptInCohortConfigurationValues.cohorts.values.find(c => c.id == id)

  implicit val pathBinder: PathBindable[Option[OptInCohort]] = PathBindable.bindableInt.transform(
    fromId,
    _.map(_.id).getOrElse(throw new IllegalArgumentException("Cannot generate a URL for an unknown Cohort")))

  implicit val cohortNumWrites = Writes[OptInCohort] { optInCohort =>
    JsNumber(optInCohort.id)
  }

  val cohortWrites = Writes[OptInCohort] { optInCohort =>
    Json.obj(
      "id"           -> optInCohort.id,
      "name"         -> optInCohort.name,
      "pageType"     -> optInCohort.pageType,
      "majorVersion" -> optInCohort.majorVersion,
      "minorVersion" -> optInCohort.minorVersion,
      "description"  -> optInCohort.description,
      "date"         -> optInCohort.date.toString()
    )
  }
  val listCohortWrites: Writes[List[OptInCohort]] = Writes { seq =>
    JsArray(seq.map(cohortWrites.writes))
  }
}

object IPage7 extends OptInCohort {
  override val id: Int = 7
  override val name: String = "IPage7"
  override val terms: TermsType = GenericTerms
  override val pageType: PageType = PageType.IPage
  override val majorVersion: Int = 0
  override val minorVersion: Int = 0
  override val description: String = ""
  override val date: LocalDate = new LocalDate("2020-05-12")
}

object IPage8 extends OptInCohort {
  override val id: Int = 8
  override val name: String = "IPage8"
  override val terms: TermsType = GenericTerms
  override val pageType: PageType = PageType.IPage
  override val majorVersion: Int = 1
  override val minorVersion: Int = 0
  override val description: String = "SOL changes to wording to improve litigation cases"
  override val date: LocalDate = new LocalDate("2020-05-12")
}

object TCPage9 extends OptInCohort {
  override val id: Int = 9
  override val name: String = "TCPage9"
  override val terms: TermsType = TaxCreditsTerms
  override val pageType: PageType = PageType.TCPage
  override val majorVersion: Int = 0
  override val minorVersion: Int = 0
  override val description: String = ""
  override val date: LocalDate = new LocalDate("2020-05-12")
}

object ReOptInPage10 extends OptInCohort {
  override val id: Int = 10
  override val name: String = "ReOptInPage10"
  override val terms: TermsType = GenericTerms
  override val pageType: PageType = PageType.ReOptInPage
  override val majorVersion: Int = 1
  override val minorVersion: Int = 0
  override val description: String = ""
  override val date: LocalDate = new LocalDate("2020-07-02")
}

object CohortCurrent {
  val ipage = IPage8
  val tcpage = TCPage9
  val reoptinpage = ReOptInPage10
}
