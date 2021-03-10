/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package partial.paperless.tc

import _root_.helpers.{ ConfigHelper, LanguageHelper }
import org.jsoup.Jsoup
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import partial.paperless.tc.html.problem
import play.api.Application
import play.api.data.FormError
import play.api.test.FakeRequest

class ProblemSpec extends PlaySpec with GuiceOneAppPerSuite with LanguageHelper with ConfigHelper {

  override implicit lazy val app: Application = fakeApp

  "problem partial" should {
    "render the correct content in english" in {
      val errors = Seq((FormError("ErrorKey", Seq("Error Message"), Seq()), "Outer Error Message"))
      val document = Jsoup.parse(problem(errors)(FakeRequest(), messagesInEnglish()).toString())

      document.getElementById("error-summary-heading").text() mustBe "There is a problem"
    }

    "render the correct content in welsh" in {
      val errors = Seq((FormError("ErrorKey", Seq("Error Message"), Seq()), "Outer Error Message"))
      val document = Jsoup.parse(problem(errors)(welshRequest, messagesInWelsh()).toString())

      document.getElementById("error-summary-heading").text() mustBe "Mae yna broblem"
    }
  }
}
