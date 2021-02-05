/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package controllers.internal

import connectors.GenericTerms
import model.PageType
import org.joda.time.LocalDate
import org.scalatestplus.play.PlaySpec

class ReadOnlyIosOptInPage35CohortSpec extends PlaySpec {

  "IosOptInPage35 OptInCohort" should {
    "never change fields values " in {
      val cohortUnderTest = IosOptInPage35

      withClue("id") {
        cohortUnderTest.id mustBe 35
      }
      withClue("name") {
        cohortUnderTest.name mustBe "IosOptInPage35"
      }
      withClue("terms") {
        cohortUnderTest.terms mustBe GenericTerms
      }
      withClue("pageType") {
        cohortUnderTest.pageType mustBe (PageType.IosOptInPage)
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
        cohortUnderTest.date mustBe new LocalDate("2020-03-31")
      }
    }
  }

}
