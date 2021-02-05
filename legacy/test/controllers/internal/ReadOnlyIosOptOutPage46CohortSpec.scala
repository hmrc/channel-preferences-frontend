/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package controllers.internal

import connectors.GenericTerms
import model.PageType
import org.joda.time.LocalDate
import org.scalatestplus.play.PlaySpec

class ReadOnlyIosOptOutPage46CohortSpec extends PlaySpec {

  "IosOptOut46 OptInCohort" should {
    "never change fields values " in {
      val cohortUnderTest = IosOptOutPage46

      withClue("id") {
        cohortUnderTest.id mustBe 46
      }
      withClue("name") {
        cohortUnderTest.name mustBe "IosOptOutPage46"
      }
      withClue("terms") {
        cohortUnderTest.terms mustBe GenericTerms
      }
      withClue("pageType") {
        cohortUnderTest.pageType mustBe (PageType.IosOptOutPage)
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
        cohortUnderTest.date mustBe new LocalDate("2020-01-01")
      }
    }
  }

}
