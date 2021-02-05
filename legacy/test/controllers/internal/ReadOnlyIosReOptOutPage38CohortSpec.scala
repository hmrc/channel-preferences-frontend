/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package controllers.internal

import connectors.GenericTerms
import model.PageType
import org.joda.time.LocalDate
import org.scalatestplus.play.PlaySpec

class ReadOnlyIosReOptOutPage38CohortSpec extends PlaySpec {

  "IosReOptOut38 OptInCohort" should {
    "never change fields values " in {
      val cohortUnderTest = IosReOptOutPage38

      withClue("id") {
        cohortUnderTest.id mustBe 38
      }
      withClue("name") {
        cohortUnderTest.name mustBe "IosReOptOutPage38"
      }
      withClue("terms") {
        cohortUnderTest.terms mustBe GenericTerms
      }
      withClue("pageType") {
        cohortUnderTest.pageType mustBe (PageType.IosReOptOutPage)
      }
      withClue("majorVersion") {
        cohortUnderTest.majorVersion mustBe 1
      }
      withClue("minorVersion") {
        cohortUnderTest.minorVersion mustBe 1
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
