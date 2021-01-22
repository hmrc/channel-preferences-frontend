/*
 * Copyright 2021 HM Revenue & Customs
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

  implicit val cohortNumWrites: Writes[OptInCohort] = Writes[OptInCohort] { optInCohort =>
    JsNumber(optInCohort.id)
  }

  val cohortWrites: Writes[OptInCohort] = Writes[OptInCohort] { optInCohort =>
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

object ReOptInPage52 extends OptInCohort {
  override val id: Int = 52
  override val name: String = "ReOptInPage52"
  override val terms: TermsType = GenericTerms
  override val pageType: PageType = PageType.ReOptInPage
  override val majorVersion: Int = 1
  override val minorVersion: Int = 1
  override val description: String = ""
  override val date: LocalDate = new LocalDate("2020-10-21")
}

object CYSConfirmPage47 extends OptInCohort {
  override val id: Int = 47
  override val name: String = "CYSConfirmPage47"
  override val terms: TermsType = GenericTerms
  override val pageType: PageType = PageType.CYSConfirmPage
  override val majorVersion: Int = 1
  override val minorVersion: Int = 0
  override val description: String = ""
  override val date: LocalDate = new LocalDate("2020-09-07")
}

// Android 0.0 ////////////////////////////////////////////////////////////////
object AndroidOptInPage11 extends OptInCohort {
  override val id: Int = 11
  override val name: String = "AndroidOptInPage11"
  override val terms: TermsType = GenericTerms
  override val pageType: PageType = PageType.AndroidOptInPage
  override val majorVersion: Int = 0
  override val minorVersion: Int = 0
  override val description: String = ""
  override val date: LocalDate = new LocalDate("2019-12-05")
}
object AndroidOptOutPage12 extends OptInCohort {
  override val id: Int = 12
  override val name: String = "AndroidOptOutPage12"
  override val terms: TermsType = GenericTerms
  override val pageType: PageType = PageType.AndroidOptOutPage
  override val majorVersion: Int = 0
  override val minorVersion: Int = 0
  override val description: String = ""
  override val date: LocalDate = new LocalDate("2020-01-01")
}
object AndroidReOptInPage13 extends OptInCohort {
  override val id: Int = 13
  override val name: String = "AndroidReOptInPage13"
  override val terms: TermsType = GenericTerms
  override val pageType: PageType = PageType.AndroidReOptInPage
  override val majorVersion: Int = 0
  override val minorVersion: Int = 0
  override val description: String = ""
  override val date: LocalDate = new LocalDate("2020-01-01")
}
object AndroidReOptOutPage14 extends OptInCohort {
  override val id: Int = 14
  override val name: String = "AndroidReOptOutPage14"
  override val terms: TermsType = GenericTerms
  override val pageType: PageType = PageType.AndroidReOptOutPage
  override val majorVersion: Int = 0
  override val minorVersion: Int = 0
  override val description: String = ""
  override val date: LocalDate = new LocalDate("2020-01-01")
}
// end of Android 0.0 ///////////////////////////////////////////////////////

// Android 1.0 ////////////////////////////////////////////////////////////////
object AndroidOptInPage15 extends OptInCohort {
  override val id: Int = 15
  override val name: String = "AndroidOptInPage15"
  override val terms: TermsType = GenericTerms
  override val pageType: PageType = PageType.AndroidOptInPage
  override val majorVersion: Int = 1
  override val minorVersion: Int = 0
  override val description: String = ""
  override val date: LocalDate = new LocalDate("2020-01-16")
}
object AndroidOptOutPage16 extends OptInCohort {
  override val id: Int = 16
  override val name: String = "AndroidOptOutPage16"
  override val terms: TermsType = GenericTerms
  override val pageType: PageType = PageType.AndroidOptOutPage
  override val majorVersion: Int = 1
  override val minorVersion: Int = 0
  override val description: String = ""
  override val date: LocalDate = new LocalDate("2020-01-01")
}
object AndroidReOptInPage17 extends OptInCohort {
  override val id: Int = 17
  override val name: String = "AndroidReOptInPage17"
  override val terms: TermsType = GenericTerms
  override val pageType: PageType = PageType.AndroidReOptInPage
  override val majorVersion: Int = 1
  override val minorVersion: Int = 0
  override val description: String = ""
  override val date: LocalDate = new LocalDate("2020-01-01")
}
object AndroidReOptOutPage18 extends OptInCohort {
  override val id: Int = 18
  override val name: String = "AndroidReOptOutPage18"
  override val terms: TermsType = GenericTerms
  override val pageType: PageType = PageType.AndroidReOptOutPage
  override val majorVersion: Int = 1
  override val minorVersion: Int = 0
  override val description: String = ""
  override val date: LocalDate = new LocalDate("2020-01-01")
}

// Android 1.1 ////////////////////////////////////////////////////////////////
object AndroidOptInPage19 extends OptInCohort {
  override val id: Int = 19
  override val name: String = "AndroidOptInPage19"
  override val terms: TermsType = GenericTerms
  override val pageType: PageType = PageType.AndroidOptInPage
  override val majorVersion: Int = 1
  override val minorVersion: Int = 1
  override val description: String = ""
  override val date: LocalDate = new LocalDate("2020-03-31")
}
object AndroidOptOutPage20 extends OptInCohort {
  override val id: Int = 20
  override val name: String = "AndroidOptOutPage20"
  override val terms: TermsType = GenericTerms
  override val pageType: PageType = PageType.AndroidOptOutPage
  override val majorVersion: Int = 1
  override val minorVersion: Int = 1
  override val description: String = ""
  override val date: LocalDate = new LocalDate("2020-01-01")
}
object AndroidReOptInPage21 extends OptInCohort {
  override val id: Int = 21
  override val name: String = "AndroidReOptInPage21"
  override val terms: TermsType = GenericTerms
  override val pageType: PageType = PageType.AndroidReOptInPage
  override val majorVersion: Int = 1
  override val minorVersion: Int = 1
  override val description: String = ""
  override val date: LocalDate = new LocalDate("2020-01-01")
}
object AndroidReOptOutPage22 extends OptInCohort {
  override val id: Int = 22
  override val name: String = "AndroidReOptOutPage22"
  override val terms: TermsType = GenericTerms
  override val pageType: PageType = PageType.AndroidReOptOutPage
  override val majorVersion: Int = 1
  override val minorVersion: Int = 1
  override val description: String = ""
  override val date: LocalDate = new LocalDate("2020-01-01")
}
// end of Android 1.1 ///////////////////////////////////////////////////////

// Android 1.2 ////////////////////////////////////////////////////////////////
object AndroidOptInPage23 extends OptInCohort {
  override val id: Int = 23
  override val name: String = "AndroidOptInPage23"
  override val terms: TermsType = GenericTerms
  override val pageType: PageType = PageType.AndroidOptInPage
  override val majorVersion: Int = 1
  override val minorVersion: Int = 2
  override val description: String = ""
  override val date: LocalDate = new LocalDate("2020-07-09")
}
object AndroidOptOutPage24 extends OptInCohort {
  override val id: Int = 24
  override val name: String = "AndroidOptOutPage24"
  override val terms: TermsType = GenericTerms
  override val pageType: PageType = PageType.AndroidOptOutPage
  override val majorVersion: Int = 1
  override val minorVersion: Int = 2
  override val description: String = ""
  override val date: LocalDate = new LocalDate("2020-01-01")
}
object AndroidReOptInPage25 extends OptInCohort {
  override val id: Int = 25
  override val name: String = "AndroidReOptInPage25"
  override val terms: TermsType = GenericTerms
  override val pageType: PageType = PageType.AndroidReOptInPage
  override val majorVersion: Int = 1
  override val minorVersion: Int = 2
  override val description: String = ""
  override val date: LocalDate = new LocalDate("2020-01-01")
}
object AndroidReOptOutPage26 extends OptInCohort {
  override val id: Int = 26
  override val name: String = "AndroidReOptOutPage26"
  override val terms: TermsType = GenericTerms
  override val pageType: PageType = PageType.AndroidReOptOutPage
  override val majorVersion: Int = 1
  override val minorVersion: Int = 2
  override val description: String = ""
  override val date: LocalDate = new LocalDate("2020-01-01")
}
// end of Android 1.2 ///////////////////////////////////////////////////////

// Ios 0.0 ////////////////////////////////////////////////////////////////
object IosOptInPage27 extends OptInCohort {
  override val id: Int = 27
  override val name: String = "IosOptInPage27"
  override val terms: TermsType = GenericTerms
  override val pageType: PageType = PageType.IosOptInPage
  override val majorVersion: Int = 0
  override val minorVersion: Int = 0
  override val description: String = ""
  override val date: LocalDate = new LocalDate("2019-12-10")
}
object IosOptOutPage28 extends OptInCohort {
  override val id: Int = 28
  override val name: String = "IosOptOutPage28"
  override val terms: TermsType = GenericTerms
  override val pageType: PageType = PageType.IosOptOutPage
  override val majorVersion: Int = 0
  override val minorVersion: Int = 0
  override val description: String = ""
  override val date: LocalDate = new LocalDate("2020-01-01")
}
object IosReOptInPage29 extends OptInCohort {
  override val id: Int = 29
  override val name: String = "IosReOptInPage29"
  override val terms: TermsType = GenericTerms
  override val pageType: PageType = PageType.IosReOptInPage
  override val majorVersion: Int = 0
  override val minorVersion: Int = 0
  override val description: String = ""
  override val date: LocalDate = new LocalDate("2020-01-01")
}
object IosReOptOutPage30 extends OptInCohort {
  override val id: Int = 30
  override val name: String = "IosReOptOutPage30"
  override val terms: TermsType = GenericTerms
  override val pageType: PageType = PageType.IosReOptOutPage
  override val majorVersion: Int = 0
  override val minorVersion: Int = 0
  override val description: String = ""
  override val date: LocalDate = new LocalDate("2020-01-01")
}
// end of Ios 0.0 ///////////////////////////////////////////////////////

// Ios 1.0 ////////////////////////////////////////////////////////////////
object IosOptInPage31 extends OptInCohort {
  override val id: Int = 31
  override val name: String = "IosOptInPage31"
  override val terms: TermsType = GenericTerms
  override val pageType: PageType = PageType.IosOptInPage
  override val majorVersion: Int = 1
  override val minorVersion: Int = 0
  override val description: String = ""
  override val date: LocalDate = new LocalDate("2020-01-22")
}
object IosOptOutPage32 extends OptInCohort {
  override val id: Int = 32
  override val name: String = "IosOptOutPage32"
  override val terms: TermsType = GenericTerms
  override val pageType: PageType = PageType.IosOptOutPage
  override val majorVersion: Int = 1
  override val minorVersion: Int = 0
  override val description: String = ""
  override val date: LocalDate = new LocalDate("2020-01-01")
}
object IosReOptInPage33 extends OptInCohort {
  override val id: Int = 33
  override val name: String = "IosReOptInPage33"
  override val terms: TermsType = GenericTerms
  override val pageType: PageType = PageType.IosReOptInPage
  override val majorVersion: Int = 1
  override val minorVersion: Int = 0
  override val description: String = ""
  override val date: LocalDate = new LocalDate("2020-01-01")
}
object IosReOptOutPage34 extends OptInCohort {
  override val id: Int = 34
  override val name: String = "IosReOptOutPage34"
  override val terms: TermsType = GenericTerms
  override val pageType: PageType = PageType.IosReOptOutPage
  override val majorVersion: Int = 1
  override val minorVersion: Int = 0
  override val description: String = ""
  override val date: LocalDate = new LocalDate("2020-01-01")
}

// Ios 1.1 ////////////////////////////////////////////////////////////////
object IosOptInPage35 extends OptInCohort {
  override val id: Int = 35
  override val name: String = "IosOptInPage35"
  override val terms: TermsType = GenericTerms
  override val pageType: PageType = PageType.IosOptInPage
  override val majorVersion: Int = 1
  override val minorVersion: Int = 1
  override val description: String = ""
  override val date: LocalDate = new LocalDate("2020-03-31")
}
object IosOptOutPage36 extends OptInCohort {
  override val id: Int = 36
  override val name: String = "IosOptOutPage36"
  override val terms: TermsType = GenericTerms
  override val pageType: PageType = PageType.IosOptOutPage
  override val majorVersion: Int = 1
  override val minorVersion: Int = 1
  override val description: String = ""
  override val date: LocalDate = new LocalDate("2020-01-01")
}
object IosReOptInPage37 extends OptInCohort {
  override val id: Int = 37
  override val name: String = "IosReOptInPage37"
  override val terms: TermsType = GenericTerms
  override val pageType: PageType = PageType.IosReOptInPage
  override val majorVersion: Int = 1
  override val minorVersion: Int = 1
  override val description: String = ""
  override val date: LocalDate = new LocalDate("2020-01-01")
}
object IosReOptOutPage38 extends OptInCohort {
  override val id: Int = 38
  override val name: String = "IosReOptOutPage38"
  override val terms: TermsType = GenericTerms
  override val pageType: PageType = PageType.IosReOptOutPage
  override val majorVersion: Int = 1
  override val minorVersion: Int = 1
  override val description: String = ""
  override val date: LocalDate = new LocalDate("2020-01-01")
}
// end of Ios 1.1 ///////////////////////////////////////////////////////

// Ios 1.2 ////////////////////////////////////////////////////////////////
object IosOptInPage39 extends OptInCohort {
  override val id: Int = 39
  override val name: String = "IosOptInPage39"
  override val terms: TermsType = GenericTerms
  override val pageType: PageType = PageType.IosOptInPage
  override val majorVersion: Int = 1
  override val minorVersion: Int = 2
  override val description: String = ""
  override val date: LocalDate = new LocalDate("2020-07-07")
}
object IosOptOutPage40 extends OptInCohort {
  override val id: Int = 40
  override val name: String = "IosOptOutPage40"
  override val terms: TermsType = GenericTerms
  override val pageType: PageType = PageType.IosOptOutPage
  override val majorVersion: Int = 1
  override val minorVersion: Int = 2
  override val description: String = ""
  override val date: LocalDate = new LocalDate("2020-01-01")
}
object IosReOptInPage41 extends OptInCohort {
  override val id: Int = 41
  override val name: String = "IosReOptInPage41"
  override val terms: TermsType = GenericTerms
  override val pageType: PageType = PageType.IosReOptInPage
  override val majorVersion: Int = 1
  override val minorVersion: Int = 2
  override val description: String = ""
  override val date: LocalDate = new LocalDate("2020-01-01")
}
object IosReOptOutPage42 extends OptInCohort {
  override val id: Int = 42
  override val name: String = "IosReOptOutPage42"
  override val terms: TermsType = GenericTerms
  override val pageType: PageType = PageType.IosReOptOutPage
  override val majorVersion: Int = 1
  override val minorVersion: Int = 2
  override val description: String = ""
  override val date: LocalDate = new LocalDate("2020-01-01")
}
// end of Ios 1.2 ///////////////////////////////////////////////////////

// Android 1.3 ////////////////////////////////////////////////////////////////
object AndroidOptInPage43 extends OptInCohort {
  override val id: Int = 43
  override val name: String = "AndroidOptInPage43"
  override val terms: TermsType = GenericTerms
  override val pageType: PageType = PageType.AndroidOptInPage
  override val majorVersion: Int = 1
  override val minorVersion: Int = 3
  override val description: String = ""
  override val date: LocalDate = new LocalDate("2020-01-01")
}
object AndroidOptOutPage44 extends OptInCohort {
  override val id: Int = 44
  override val name: String = "AndroidOptOutPage44"
  override val terms: TermsType = GenericTerms
  override val pageType: PageType = PageType.AndroidOptOutPage
  override val majorVersion: Int = 1
  override val minorVersion: Int = 3
  override val description: String = ""
  override val date: LocalDate = new LocalDate("2020-01-01")
}
object AndroidReOptInPage48 extends OptInCohort {
  override val id: Int = 48
  override val name: String = "AndroidReOptInPage48"
  override val terms: TermsType = GenericTerms
  override val pageType: PageType = PageType.AndroidReOptInPage
  override val majorVersion: Int = 1
  override val minorVersion: Int = 3
  override val description: String = ""
  override val date: LocalDate = new LocalDate("2020-08-01")
}
object AndroidReOptOutPage49 extends OptInCohort {
  override val id: Int = 49
  override val name: String = "AndroidReOptOutPage49"
  override val terms: TermsType = GenericTerms
  override val pageType: PageType = PageType.AndroidReOptOutPage
  override val majorVersion: Int = 1
  override val minorVersion: Int = 3
  override val description: String = ""
  override val date: LocalDate = new LocalDate("2020-08-01")
}
// end of Android 1.3 ///////////////////////////////////////////////////////

// Ios 1.3 ////////////////////////////////////////////////////////////////
object IosOptInPage45 extends OptInCohort {
  override val id: Int = 45
  override val name: String = "IosOptInPage45"
  override val terms: TermsType = GenericTerms
  override val pageType: PageType = PageType.IosOptInPage
  override val majorVersion: Int = 1
  override val minorVersion: Int = 3
  override val description: String = ""
  override val date: LocalDate = new LocalDate("2020-01-01")
}
object IosOptOutPage46 extends OptInCohort {
  override val id: Int = 46
  override val name: String = "IosOptOutPage46"
  override val terms: TermsType = GenericTerms
  override val pageType: PageType = PageType.IosOptOutPage
  override val majorVersion: Int = 1
  override val minorVersion: Int = 3
  override val description: String = ""
  override val date: LocalDate = new LocalDate("2020-01-01")
}
object IosReOptInPage50 extends OptInCohort {
  override val id: Int = 50
  override val name: String = "IosReOptInPage50"
  override val terms: TermsType = GenericTerms
  override val pageType: PageType = PageType.IosReOptInPage
  override val majorVersion: Int = 1
  override val minorVersion: Int = 3
  override val description: String = ""
  override val date: LocalDate = new LocalDate("2020-08-01")
}
object IosReOptOutPage51 extends OptInCohort {
  override val id: Int = 51
  override val name: String = "IosReOptOutPage51"
  override val terms: TermsType = GenericTerms
  override val pageType: PageType = PageType.IosReOptOutPage
  override val majorVersion: Int = 1
  override val minorVersion: Int = 3
  override val description: String = ""
  override val date: LocalDate = new LocalDate("2020-08-01")
}
// end of Ios 1.3 ///////////////////////////////////////////////////////

object CohortCurrent {
  val ipage: OptInCohort = IPage8
  val tcpage: OptInCohort = TCPage9
  val reoptinpage: OptInCohort = ReOptInPage52
  val cysConfirmPage: OptInCohort = CYSConfirmPage47
}
