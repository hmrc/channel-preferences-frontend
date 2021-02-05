/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package controllers.internal

import connectors.GenericTerms
import model.PageType
import org.joda.time.LocalDate
import org.scalatestplus.play.PlaySpec

class ReadOnlyIosReOptInPage50CohortSpec extends PlaySpec {
  "IosReOptInPage50 OptInCohort" should {
    "never change fields values " in {
      val cohortUnderTest = IosReOptInPage50

      withClue("id") {
        cohortUnderTest.id mustBe 50
      }
      withClue("name") {
        cohortUnderTest.name mustBe "IosReOptInPage50"
      }
      withClue("terms") {
        cohortUnderTest.terms mustBe GenericTerms
      }
      withClue("pageType") {
        cohortUnderTest.pageType mustBe (PageType.IosReOptInPage)
      }
      withClue("majorVersion") {
        cohortUnderTest.majorVersion mustBe 1
      }
      withClue("minorVersion") {
        cohortUnderTest.minorVersion mustBe 3
      }
      withClue("description") {
        cohortUnderTest.description mustBe ""
      }
      withClue("date") {
        cohortUnderTest.date mustBe new LocalDate("2020-08-01")
      }
    }
  }
}
