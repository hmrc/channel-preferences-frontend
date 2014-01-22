package controllers.common.support

import uk.gov.hmrc.common.BaseSpec
import play.api.test.{FakeRequest, WithApplication}
import play.api.test.Helpers._
import org.jsoup.Jsoup
import uk.gov.hmrc.common.microservice.audit.AuditConnector
import uk.gov.hmrc.common.microservice.deskpro.HmrcDeskproConnector
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import org.mockito.Mockito._
import scala.concurrent.Future
import org.mockito.Matchers
import org.mockito.Matchers.{eq => meq, any}
import controllers.common.actions.HeaderCarrier
import scala.Some
import play.api.test.FakeApplication
import play.api.mvc.Request
import uk.gov.hmrc.common.microservice.deskpro.domain.TicketId

class ProblemReportsControllerSpec extends BaseSpec {

  def generateRequest(javascriptEnabled: Boolean = true) = FakeRequest()
    .withHeaders(("referer", "/contact/problem_reports"), ("User-Agent", "iAmAUserAgent"))
    .withFormUrlEncodedBody("report-name" -> "John Densmore", "report-email" -> "name@mail.com", "report-telephone" -> "012345678",
    "report-action" -> "Some Action", "report-error" -> "Some Error", "isJavascript" -> javascriptEnabled.toString)

  def generateInvalidRequest(javascriptEnabled: Boolean = true) = FakeRequest()
    .withHeaders(("referer", "/contact/problem_reports"), ("User-Agent", "iAmAUserAgent"))
    .withFormUrlEncodedBody("isJavascript" -> javascriptEnabled.toString)

  "Reporting a problem" should {
    "return 200 and a valid json for a valid request and js is enabled" in new ProblemReportsControllerApplication {

      when(hmrcDeskproConnector.createTicket(meq("John Densmore"), meq("name@mail.com"), meq("Support Request"), meq(controller.problemMessage("Some Action", "Some Error")), meq("/contact/problem_reports"), meq(true), any[Request[AnyRef]](), meq(None))(Matchers.any(classOf[HeaderCarrier]))).thenReturn(Future.successful(Some(TicketId(123))))

      val result = controller.doReport(generateRequest())

      status(result) should be(200)

      val message = contentAsJson(result).\("message").as[String]
      contentAsJson(result).\("status").as[String] shouldBe "OK"
      message contains "<h2 id=\"feedback-thank-you-header\">Thank you</h2>"
      message contains "Your reference number is 123."
      message contains "You'll get a response within 24 hours."
    }

    "return 200 and a valid html page for a valid request and js is not enabled" in new ProblemReportsControllerApplication {

      when(hmrcDeskproConnector.createTicket(meq("John Densmore"), meq("name@mail.com"), meq("Support Request"), meq(controller.problemMessage("Some Action", "Some Error")), meq("/contact/problem_reports"), meq(false), any[Request[AnyRef]](), meq(None))(Matchers.any(classOf[HeaderCarrier]))).thenReturn(Future.successful(Some(TicketId(123))))

      val result = controller.doReport(generateRequest(javascriptEnabled = false))

      status(result) should be(200)

      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("report-confirmation") should not be null
    }

    "return 200 and a valid html page for invalid input and js is not enabled" in new ProblemReportsControllerApplication {

      val result = controller.doReport(generateInvalidRequest( javascriptEnabled = false))

      status(result) should be(200)
      verifyZeroInteractions(hmrcDeskproConnector)

      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("report-confirmation-no-data") should not be null
    }

    "return 400 and a valid json for invalid input and js is enabled" in new ProblemReportsControllerApplication {

      val result = controller.doReport(generateInvalidRequest())

      status(result) should be(400)
      verifyZeroInteractions(hmrcDeskproConnector)

      contentAsJson(result).\("status").as[String] shouldBe "ERROR"
    }
  }
}

class ProblemReportsControllerApplication extends WithApplication(FakeApplication()) with MockitoSugar {
  val auditConnector: AuditConnector = mock[AuditConnector]
  val hmrcDeskproConnector: HmrcDeskproConnector = mock[HmrcDeskproConnector]
  implicit val authConnector: AuthConnector = mock[AuthConnector]
  val controller = new ProblemReportsController(auditConnector, hmrcDeskproConnector)
}
