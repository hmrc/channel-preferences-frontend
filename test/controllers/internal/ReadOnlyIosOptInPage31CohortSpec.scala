/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package controllers.internal

import connectors.GenericTerms
import model.PageType
import org.joda.time.LocalDate
import org.scalatestplus.play.PlaySpec

class ReadOnlyIosOptInPage31CohortSpec extends PlaySpec {

  "IosOptInPage31 OptInCohort" should {
    "never change fields values " in {
      val cohortUnderTest = IosOptInPage31

      withClue("id") {
        cohortUnderTest.id mustBe 31
      }
      withClue("name") {
        cohortUnderTest.name mustBe "IosOptInPage31"
      }
      withClue("terms") {
        cohortUnderTest.terms mustBe GenericTerms
      }
      withClue("pageType") {
        cohortUnderTest.pageType mustBe (PageType.IosOptInPage)
      }
      withClue("majorVersion") {
        cohortUnderTest.majorVersion mustBe 1
      }
      withClue("minorVersion") {
        cohortUnderTest.minorVersion mustBe 0
      }
      withClue("description") {
        cohortUnderTest.description mustBe ""
      }
      withClue("date") {
        cohortUnderTest.date mustBe new LocalDate("2020-01-22")
      }
    }
  }

}
