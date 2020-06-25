/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package controllers.internal

import connectors.TaxCreditsTerms
import model.PageType
import org.joda.time.LocalDate
import org.scalatestplus.play.PlaySpec

class ReadOnlyTCPage9CohortSpec extends PlaySpec {

  "TCage9 OptInCohort" should {
    "never change fields values " in {
      val cohortUnderTest = TCPage9

      withClue("id") {
        cohortUnderTest.id mustBe (9)
      }
      withClue("name") {
        cohortUnderTest.name mustBe ("TCPage9")
      }
      withClue("terms") {
        cohortUnderTest.terms mustBe (TaxCreditsTerms)
      }
      withClue("pageType") {
        cohortUnderTest.pageType mustBe (PageType.TCPage)
      }
      withClue("majorVersion") {
        cohortUnderTest.majorVersion mustBe (0)
      }

      withClue("description") {
        cohortUnderTest.description mustBe ("")
      }
      withClue("date") {
        cohortUnderTest.date mustBe new LocalDate("2020-05-12")
      }
    }
  }

}
