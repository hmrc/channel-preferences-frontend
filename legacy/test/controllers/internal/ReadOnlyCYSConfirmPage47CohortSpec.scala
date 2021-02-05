/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package controllers.internal

import connectors.GenericTerms
import model.PageType
import org.joda.time.LocalDate
import org.scalatestplus.play.PlaySpec

class ReadOnlyCYSConfirmPage47CohortSpec extends PlaySpec {

  "CYSConfirmPage47 OptInCohort" should {
    "never change fields values " in {
      val cohortUnderTest = CYSConfirmPage47

      withClue("id") {
        cohortUnderTest.id mustBe 47
      }
      withClue("name") {
        cohortUnderTest.name mustBe "CYSConfirmPage47"
      }
      withClue("terms") {
        cohortUnderTest.terms mustBe GenericTerms
      }
      withClue("pageType") {
        cohortUnderTest.pageType mustBe PageType.CYSConfirmPage
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
        cohortUnderTest.date mustBe new LocalDate("2020-09-07")
      }
    }
  }

}
