/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package controllers.internal

import connectors.GenericTerms
import model.PageType
import org.joda.time.LocalDate
import org.scalatestplus.play.PlaySpec

class ReadOnlyIPage7CohortSpec extends PlaySpec {

  "IPage7 OptInCohort" should {
    "never change fields values " in {
      val cohortUnderTest = IPage7

      withClue("id") {
        cohortUnderTest.id mustBe (7)
      }
      withClue("name") {
        cohortUnderTest.name mustBe ("IPage7")
      }
      withClue("terms") {
        cohortUnderTest.terms mustBe (GenericTerms)
      }
      withClue("pageType") {
        cohortUnderTest.pageType mustBe (PageType.IPage)
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
        cohortUnderTest.date mustBe new LocalDate("2020-05-12")
      }
    }
  }

}
