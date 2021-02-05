/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package controllers.internal

import connectors.GenericTerms
import model.PageType
import org.joda.time.LocalDate
import org.scalatestplus.play.PlaySpec

class ReadOnlyAndroidReOptInPage21CohortSpec extends PlaySpec {

  "AndroidReOptInPage21 OptInCohort" should {
    "never change fields values " in {
      val cohortUnderTest = AndroidReOptInPage21

      withClue("id") {
        cohortUnderTest.id mustBe 21
      }
      withClue("name") {
        cohortUnderTest.name mustBe "AndroidReOptInPage21"
      }
      withClue("terms") {
        cohortUnderTest.terms mustBe GenericTerms
      }
      withClue("pageType") {
        cohortUnderTest.pageType mustBe (PageType.AndroidReOptInPage)
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
