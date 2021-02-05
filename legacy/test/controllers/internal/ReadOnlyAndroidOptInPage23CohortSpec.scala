/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package controllers.internal

import connectors.GenericTerms
import model.PageType
import org.joda.time.LocalDate
import org.scalatestplus.play.PlaySpec

class ReadOnlyAndroidOptInPage23CohortSpec extends PlaySpec {

  "AndroidOptInPage23 OptInCohort" should {
    "never change fields values " in {
      val cohortUnderTest = AndroidOptInPage23

      withClue("id") {
        cohortUnderTest.id mustBe 23
      }
      withClue("name") {
        cohortUnderTest.name mustBe "AndroidOptInPage23"
      }
      withClue("terms") {
        cohortUnderTest.terms mustBe GenericTerms
      }
      withClue("pageType") {
        cohortUnderTest.pageType mustBe (PageType.AndroidOptInPage)
      }
      withClue("majorVersion") {
        cohortUnderTest.majorVersion mustBe 1
      }
      withClue("minorVersion") {
        cohortUnderTest.minorVersion mustBe 2
      }
      withClue("description") {
        cohortUnderTest.description mustBe ""
      }
      withClue("date") {
        cohortUnderTest.date mustBe new LocalDate("2020-07-09")
      }
    }
  }

}
