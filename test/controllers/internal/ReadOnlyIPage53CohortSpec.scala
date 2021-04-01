/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package controllers.internal

import connectors.GenericTerms
import model.JourneyType.MultiPage1
import model.PageType
import org.joda.time.LocalDate
import org.scalatestplus.play.PlaySpec

class ReadOnlyIPage53CohortSpec extends PlaySpec {

  "IPage53 OptInCohort" should {
    "never change fields values " in {
      val cohortUnderTest = IPage53

      withClue("id") {
        cohortUnderTest.id mustBe 53
      }
      withClue("name") {
        cohortUnderTest.name mustBe "IPage53"
      }
      withClue("terms") {
        cohortUnderTest.terms mustBe GenericTerms
      }
      withClue("pageType") {
        cohortUnderTest.pageType mustBe (PageType.IPage)
      }
      withClue("majorVersion") {
        cohortUnderTest.majorVersion mustBe 1
      }
      withClue("minorVersion") {
        cohortUnderTest.minorVersion mustBe 1
      }
      withClue("description") {
        cohortUnderTest.description mustBe "SOL changes to wording to improve litigation cases"
      }
      withClue("date") {
        cohortUnderTest.date mustBe new LocalDate("2021-03-31")
      }
      withClue("journeyType") {
        cohortUnderTest.journeyType mustBe Some(MultiPage1)
      }
    }
  }

}
