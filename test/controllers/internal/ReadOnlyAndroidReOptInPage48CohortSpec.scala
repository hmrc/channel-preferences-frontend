/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package controllers.internal

import connectors.GenericTerms
import model.PageType
import org.joda.time.LocalDate
import org.scalatestplus.play.PlaySpec

class ReadOnlyAndroidReOptInPage48CohortSpec extends PlaySpec {

  "AndroidReOptInPage48 OptInCohort" should {
    "never change fields values " in {
      val cohortUnderTest = AndroidReOptInPage48

      withClue("id") {
        cohortUnderTest.id mustBe (48)
      }
      withClue("name") {
        cohortUnderTest.name mustBe ("AndroidReOptInPage48")
      }
      withClue("terms") {
        cohortUnderTest.terms mustBe (GenericTerms)
      }
      withClue("pageType") {
        cohortUnderTest.pageType mustBe (PageType.AndroidReOptInPage)
      }
      withClue("majorVersion") {
        cohortUnderTest.majorVersion mustBe (1)
      }
      withClue("minorVersion") {
        cohortUnderTest.minorVersion mustBe (3)
      }
      withClue("description") {
        cohortUnderTest.description mustBe ("")
      }
      withClue("date") {
        cohortUnderTest.date mustBe new LocalDate("2020-08-01")
      }
    }
  }

}
