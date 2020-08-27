/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package controllers.internal

import connectors.GenericTerms
import model.PageType
import org.joda.time.LocalDate
import org.scalatestplus.play.PlaySpec

class ReadOnlyIosOptInPage39CohortSpec extends PlaySpec {

  "IosOptInPage39 OptInCohort" should {
    "never change fields values " in {
      val cohortUnderTest = IosOptInPage39

      withClue("id") {
        cohortUnderTest.id mustBe (39)
      }
      withClue("name") {
        cohortUnderTest.name mustBe ("IosOptInPage39")
      }
      withClue("terms") {
        cohortUnderTest.terms mustBe (GenericTerms)
      }
      withClue("pageType") {
        cohortUnderTest.pageType mustBe (PageType.IosOptInPage)
      }
      withClue("majorVersion") {
        cohortUnderTest.majorVersion mustBe (1)
      }
      withClue("minorVersion") {
        cohortUnderTest.minorVersion mustBe (2)
      }
      withClue("description") {
        cohortUnderTest.description mustBe ("")
      }
      withClue("date") {
        cohortUnderTest.date mustBe new LocalDate("2020-07-07")
      }
    }
  }

}
