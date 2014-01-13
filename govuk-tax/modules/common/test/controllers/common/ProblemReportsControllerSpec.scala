package controllers.common

import uk.gov.hmrc.common.BaseSpec
import play.api.test.{FakeRequest, WithApplication, FakeApplication}
import play.api.test.Helpers._
import org.jsoup.Jsoup
import uk.gov.hmrc.common.microservice.audit.AuditConnector
import uk.gov.hmrc.common.microservice.deskpro.{TicketId, Ticket, HmrcDeskproConnector}
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import org.mockito.Mockito._
import scala.concurrent.Future
import org.mockito.Matchers
import controllers.common.actions.HeaderCarrier

class ProblemReportsControllerSpec extends BaseSpec {

  def generateRequest(action: String = "Some Action", error: String = "Some Error", javascriptEnabled: Boolean = true) = FakeRequest()
    .withHeaders(("referer", "/contact/problem_reports"), ("User-Agent", "iAmAUserAgent"))
    .withFormUrlEncodedBody("report-name" -> "John Densmore", "report-email" -> "name@mail.com", "report-telephone" -> "012345678",
    "report-action" -> action, "report-error" -> error, "isJavascript" -> javascriptEnabled.toString)


  def ticket = Ticket(
    "John Densmore",
    "name@mail.com",
    "Support Request",
    ProblemReportsController.message("Some Action", "Some Error"),
    "/contact/problem_reports",
    "Y",
    "iAmAUserAgent",
    "n/a",
    "paye|biztax",
    "n/a")

  "Reporting a problem" should {
    "return 200 and a valid json for a valid request and js is enabled" in new ProblemReportsControllerApplication {

      when(hmrcDeskproConnector.createTicket(Matchers.eq(ticket))(Matchers.any(classOf[HeaderCarrier]))).thenReturn(Future.successful(Some(TicketId(123))))

      val result = controller.report()(generateRequest())

      status(result) should be(200)

      contentAsJson(result).\("status").as[String] shouldBe "OK"
      contentAsJson(result).\("message").as[String] shouldBe "<h2 id=\"feedback-thank-you-header\">Thank you for your help. Your support reference number is <span id=\"ticketId\">123</span></h2> <p>If you have more extensive feedback, please visit the <a href='/contact'>contact page</a>.</p>"
    }

    "return 200 and a valid html page for a valid request and js is not enabled" in new ProblemReportsControllerApplication {

      when(hmrcDeskproConnector.createTicket(Matchers.eq(ticket.copy(javascriptEnabled = "N")))(Matchers.any(classOf[HeaderCarrier]))).thenReturn(Future.successful(Some(TicketId(123))))

      val result = controller.report()(generateRequest(javascriptEnabled = false))

      status(result) should be(200)

      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("report-confirmation") should not be null
    }

    "return 200 and a valid html page for an invalid action and js is not enabled" in new ProblemReportsControllerApplication {

      val result = controller.report()(generateRequest(action = "", javascriptEnabled = false))

      status(result) should be(200)
      verifyZeroInteractions(hmrcDeskproConnector)

      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("report-confirmation-no-data") should not be null
    }


    "return 200 and a valid html page for an invalid error and js is not enabled" in new ProblemReportsControllerApplication {

      val result = controller.report()(generateRequest(error = "", javascriptEnabled = false))

      status(result) should be(200)
      verifyZeroInteractions(hmrcDeskproConnector)

      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("report-confirmation-no-data") should not be null
    }

    "return 400 and a valid json for an invalid action and js is enabled" in new ProblemReportsControllerApplication {

      val result = controller.report()(generateRequest(action = ""))

      status(result) should be(400)
      verifyZeroInteractions(hmrcDeskproConnector)

      contentAsJson(result).\("status").as[String] shouldBe "ERROR"
    }

    "return 400 and a valid json for an invalid error and js is enabled" in new ProblemReportsControllerApplication {

      val result = controller.report()(generateRequest(error = ""))

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
