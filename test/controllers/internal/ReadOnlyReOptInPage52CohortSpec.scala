/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package controllers.internal

import connectors.GenericTerms
import model.PageType
import org.joda.time.LocalDate
import org.scalatestplus.play.PlaySpec

class ReadOnlyReOptInPage52CohortSpec extends PlaySpec {

  "ReOptInPage52 OptInCohort" should {
    "never change fields values " in {
      val cohortUnderTest = ReOptInPage52

      withClue("id") {
        cohortUnderTest.id mustBe (52)
      }
      withClue("name") {
        cohortUnderTest.name mustBe ("ReOptInPage52")
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
        cohortUnderTest.minorVersion mustBe (1)
      }
      withClue("description") {
        cohortUnderTest.description mustBe ("")
      }
      withClue("date") {
        cohortUnderTest.date mustBe new LocalDate("2020-10-21")
      }
    }
  }

}
