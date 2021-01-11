/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package controllers.internal

import connectors.GenericTerms
import model.PageType
import org.joda.time.LocalDate
import org.scalatestplus.play.PlaySpec

class ReadOnlyAndroidOptOutPage16CohortSpec extends PlaySpec {

  "AndroidOptOut16 OptInCohort" should {
    "never change fields values " in {
      val cohortUnderTest = AndroidOptOutPage16

      withClue("id") {
        cohortUnderTest.id mustBe (16)
      }
      withClue("name") {
        cohortUnderTest.name mustBe ("AndroidOptOutPage16")
      }
      withClue("terms") {
        cohortUnderTest.terms mustBe (GenericTerms)
      }
      withClue("pageType") {
        cohortUnderTest.pageType mustBe (PageType.AndroidOptOutPage)
      }
      withClue("majorVersion") {
        cohortUnderTest.majorVersion mustBe (1)
      }
      withClue("minorVersion") {
        cohortUnderTest.minorVersion mustBe (0)
      }
      withClue("description") {
        cohortUnderTest.description mustBe ("")
      }
      withClue("date") {
        cohortUnderTest.date mustBe new LocalDate("2020-01-01")
      }
    }
  }

}
