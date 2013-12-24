package controllers.common

import uk.gov.hmrc.common.BaseSpec
import play.api.test.{FakeRequest, WithApplication}
import play.api.test.Helpers._
import play.api.test.FakeApplication
import org.jsoup.Jsoup

class ProblemReportsControllerSpec extends BaseSpec {

  "Reporting a problem" should {
    "return 200 and a valid json for a valid request and js is enabled" in new WithApplication(FakeApplication()){
      val controller = new ProblemReportsController()

      val result = controller.report()(FakeRequest().withFormUrlEncodedBody("report-action" -> "Some Action","report-error" -> "Some Error", "isJavascript" -> "true"))

      status(result) should be(200)

      contentAsJson(result).\("status").as[String] shouldBe "OK"
      contentAsJson(result).\("message").as[String] shouldBe "<h2 id=\"feedback-thank-you-header\">Thank you for your help.</h2> <p>If you have more extensive feedback, please visit the <a href='/contact'>contact page</a>.</p>"
    }

    "return 200 and a valid html page for a valid request and js is not enabled" in new WithApplication(FakeApplication()){
      val controller = new ProblemReportsController()

      val result = controller.report()(FakeRequest().withFormUrlEncodedBody("report-action" -> "Some Action","report-error" -> "Some Error", "isJavascript" -> "false"))

      status(result) should be(200)

      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("report-confirmation") should not be null
    }

    "return 200 and a valid html page for an invalid action and js is not enabled" in new WithApplication(FakeApplication()){
      val controller = new ProblemReportsController()

      val result = controller.report()(FakeRequest().withFormUrlEncodedBody("report-action" -> "","report-error" -> "Some Error", "isJavascript" -> "false"))

      status(result) should be(200)

      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("report-confirmation-no-data") should not be null
    }


    "return 200 and a valid html page for an invalid error and js is not enabled" in new WithApplication(FakeApplication()){
      val controller = new ProblemReportsController()

      val result = controller.report()(FakeRequest().withFormUrlEncodedBody("report-action" -> "Some Action","report-error" -> "", "isJavascript" -> "false"))

      status(result) should be(200)

      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("report-confirmation-no-data") should not be null
    }

    "return 400 and a valid json for an invalid action and js is enabled" in new WithApplication(FakeApplication()){
      val controller = new ProblemReportsController()

      val result = controller.report()(FakeRequest().withFormUrlEncodedBody("report-action" -> "","report-error" -> "Some Error", "isJavascript" -> "true"))

      status(result) should be(400)

      contentAsJson(result).\("status").as[String] shouldBe "ERROR"
    }

    "return 400 and a valid json for an invalid error and js is enabled" in new WithApplication(FakeApplication()){
      val controller = new ProblemReportsController()

      val result = controller.report()(FakeRequest().withFormUrlEncodedBody("report-action" -> "Some Action","report-error" -> "", "isJavascript" -> "true"))

      status(result) should be(400)

      contentAsJson(result).\("status").as[String] shouldBe "ERROR"
    }
  }
}
