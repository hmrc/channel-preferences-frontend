/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package controllers.internal

import connectors.GenericTerms
import model.PageType
import org.joda.time.LocalDate
import org.scalatestplus.play.PlaySpec

class ReadOnlyAndroidOptInPage19CohortSpec extends PlaySpec {

  "AndroidOptInPage19 OptInCohort" should {
    "never change fields values " in {
      val cohortUnderTest = AndroidOptInPage19

      withClue("id") {
        cohortUnderTest.id mustBe (19)
      }
      withClue("name") {
        cohortUnderTest.name mustBe ("AndroidOptInPage19")
      }
      withClue("terms") {
        cohortUnderTest.terms mustBe (GenericTerms)
      }
      withClue("pageType") {
        cohortUnderTest.pageType mustBe (PageType.AndroidOptInPage)
      }
      withClue("majorVersion") {
        cohortUnderTest.majorVersion mustBe (1)
      }
      withClue("minorVersion") {
        cohortUnderTest.minorVersion mustBe (1)
      }
      withClue("description") {
        cohortUnderTest.description mustBe ("")
      }
      withClue("date") {
        cohortUnderTest.date mustBe new LocalDate("2020-03-31")
      }
    }
  }

}
