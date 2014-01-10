package controllers.common

import uk.gov.hmrc.common.BaseSpec
import play.api.test.{FakeRequest, WithApplication}
import play.api.test.Helpers._
import play.api.test.FakeApplication
import org.jsoup.Jsoup

class ContactControllerSpec extends BaseSpec {
  "Submitting Contact form" should {
    "return 200 and a valid json for a valid request and js is enabled" in new WithApplication(FakeApplication()){
      val controller = new ContactController()

      val result = controller.submit()(FakeRequest().withFormUrlEncodedBody("contact-name" -> "John Densmore", "contact-comments" -> "some comments" , "contact-email" -> "name@mail.com", "isJavascript" -> "true"))

      status(result) should be(200)

      contentAsJson(result).\("status").as[String] shouldBe "OK"
      contentAsJson(result).\("message").as[String] shouldBe "<h2 id=\"feedback-thank-you-header\">Thank you</h2> <p>Your comments will be reviewed by our customer support team.</p>"
    }

    "return 200 and a valid html page for a valid request and js is not enabled" in new WithApplication(FakeApplication()){
      val controller = new ContactController()

      val result = controller.submit()(FakeRequest().withFormUrlEncodedBody("contact-name" -> "John Densmore", "contact-comments" -> "some comments" , "contact-email" -> "name@mail.com", "isJavascript" -> "false"))

      status(result) should be(200)

      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("report-confirmation") should not be null
    }

  /*  "return 200 and a valid html page for an invalid name and js is not enabled" in new WithApplication(FakeApplication()){
      val controller = new ContactController()

      val result = controller.submit()(FakeRequest().withFormUrlEncodedBody("contact-name" -> "", "contact-comments" -> "some comments" , "contact-email" -> "name@mail.com", "isJavascript" -> "false"))

      status(result) should be(200)

      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("report-confirmation-no-data") should not be null
    }


    "return 200 and a valid html page for an invalid email and js is not enabled" in new WithApplication(FakeApplication()){
      val controller = new ContactController()

      val result = controller.submit()(FakeRequest().withFormUrlEncodedBody("contact-name" -> "John Densmore", "contact-comments" -> "some comments" , "contact-email" -> "", "isJavascript" -> "false"))

      status(result) should be(200)

      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("report-confirmation-no-data") should not be null
    }  */
  }


}
