/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package controllers.internal

import connectors.GenericTerms
import model.PageType
import org.joda.time.LocalDate
import org.scalatestplus.play.PlaySpec

class ReadOnlyAndroidReOptInPage13CohortSpec extends PlaySpec {

  "AndroidReOptInPage13 OptInCohort" should {
    "never change fields values " in {
      val cohortUnderTest = AndroidReOptInPage13

      withClue("id") {
        cohortUnderTest.id mustBe 13
      }
      withClue("name") {
        cohortUnderTest.name mustBe "AndroidReOptInPage13"
      }
      withClue("terms") {
        cohortUnderTest.terms mustBe GenericTerms
      }
      withClue("pageType") {
        cohortUnderTest.pageType mustBe (PageType.AndroidReOptInPage)
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
        cohortUnderTest.date mustBe new LocalDate("2020-01-01")
      }
    }
  }

}
