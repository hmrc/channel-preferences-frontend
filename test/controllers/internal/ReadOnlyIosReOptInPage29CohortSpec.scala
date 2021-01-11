/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package controllers.internal

import connectors.GenericTerms
import model.PageType
import org.joda.time.LocalDate
import org.scalatestplus.play.PlaySpec

class ReadOnlyIosReOptInPage29CohortSpec extends PlaySpec {

  "IosReOptInPage29 OptInCohort" should {
    "never change fields values " in {
      val cohortUnderTest = IosReOptInPage29

      withClue("id") {
        cohortUnderTest.id mustBe (29)
      }
      withClue("name") {
        cohortUnderTest.name mustBe ("IosReOptInPage29")
      }
      withClue("terms") {
        cohortUnderTest.terms mustBe (GenericTerms)
      }
      withClue("pageType") {
        cohortUnderTest.pageType mustBe (PageType.IosReOptInPage)
      }
      withClue("majorVersion") {
        cohortUnderTest.majorVersion mustBe (0)
      }
      withClue("minorVersion") {
        cohortUnderTest.minorVersion mustBe (0)
      }
      withClue("description") {
        cohortUnderTest.description mustBe ("")
      }
      withClue("date") {
        cohortUnderTest.date mustBe new LocalDate("2020-01-01")
      }
    }
  }

}
