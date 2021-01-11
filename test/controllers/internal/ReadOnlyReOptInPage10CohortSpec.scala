/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package controllers.internal

import connectors.GenericTerms
import model.PageType
import org.joda.time.LocalDate
import org.scalatestplus.play.PlaySpec

class ReadOnlyReOptInPage10CohortSpec extends PlaySpec {

  "ReOptInPage10 OptInCohort" should {
    "never change fields values " in {
      val cohortUnderTest = ReOptInPage10

      withClue("id") {
        cohortUnderTest.id mustBe (10)
      }
      withClue("name") {
        cohortUnderTest.name mustBe ("ReOptInPage10")
      }
      withClue("terms") {
        cohortUnderTest.terms mustBe (GenericTerms)
      }
      withClue("pageType") {
        cohortUnderTest.pageType mustBe (PageType.ReOptInPage)
      }
      withClue("majorVersion") {
        cohortUnderTest.majorVersion mustBe (1)
      }
      withClue("minorVersion") {
        cohortUnderTest.minorVersion mustBe (0)
      }
      withClue("description") {
        cohortUnderTest.description mustBe ("")
      }
      withClue("date") {
        cohortUnderTest.date mustBe new LocalDate("2020-07-02")
      }
    }
  }

}
