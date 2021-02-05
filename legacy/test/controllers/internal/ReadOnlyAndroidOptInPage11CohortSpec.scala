/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package controllers.internal

import connectors.GenericTerms
import model.PageType
import org.joda.time.LocalDate
import org.scalatestplus.play.PlaySpec

class ReadOnlyAndroidOptInPage11CohortSpec extends PlaySpec {

  "AndroidOptInPage11 OptInCohort" should {
    "never change fields values " in {
      val cohortUnderTest = AndroidOptInPage11

      withClue("id") {
        cohortUnderTest.id mustBe 11
      }
      withClue("name") {
        cohortUnderTest.name mustBe "AndroidOptInPage11"
      }
      withClue("terms") {
        cohortUnderTest.terms mustBe GenericTerms
      }
      withClue("pageType") {
        cohortUnderTest.pageType mustBe (PageType.AndroidOptInPage)
      }
      withClue("majorVersion") {
        cohortUnderTest.majorVersion mustBe 0
      }
      withClue("minorVersion") {
        cohortUnderTest.minorVersion mustBe 0
      }
      withClue("description") {
        cohortUnderTest.description mustBe ""
      }
      withClue("date") {
        cohortUnderTest.date mustBe new LocalDate("2019-12-05")
      }
    }
  }

}
