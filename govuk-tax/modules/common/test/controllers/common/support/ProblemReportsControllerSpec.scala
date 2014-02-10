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
import org.mockito.Matchers.{eq => meq, _}
import controllers.common.actions.HeaderCarrier
import play.api.mvc.Request
import uk.gov.hmrc.common.microservice.auth.domain.CreationAndLastModifiedDetail
import uk.gov.hmrc.common.microservice.paye.domain.PayeRoot
import uk.gov.hmrc.common.microservice.auth.domain.Accounts
import uk.gov.hmrc.common.microservice.auth.domain.Authority
import scala.Some
import uk.gov.hmrc.common.microservice.auth.domain.Credentials
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.common.microservice.domain.RegimeRoots
import play.api.test.FakeApplication
import uk.gov.hmrc.common.microservice.deskpro.domain.TicketId

class ProblemReportsControllerSpec extends BaseSpec {


  "Reporting a problem" should {

    "return 200 for an unauthenticated user" in new ProblemReportsControllerApplication {

      hrmcConnectorWillReturnTheTicketId

      val result = controller.reportUnauthenticated(request)

      status(result) should be(200)

      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("report-confirmation") should not be null
    }

    "return 200 and a valid html page for a valid request and js is not enabled for an unauthenticated user" in new ProblemReportsControllerApplication {

      hrmcConnectorWillReturnTheTicketId

      val result = controller.doReport(request)

      status(result) should be(200)

      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("report-confirmation") should not be null
    }


    "return 200 and a valid html page for a valid request and js is not enabled for an authenticated user" in new ProblemReportsControllerApplication {

      when(hmrcDeskproConnector.createTicket(meq("John Densmore"), meq("name@mail.com"), meq("Support Request"), meq(controller.problemMessage("Some Action", "Some Error")), meq("/contact/problem_reports"), meq(false), any[Request[AnyRef]]())(Matchers.any(classOf[HeaderCarrier]))).thenReturn(Future.successful(Some(TicketId(123))))

      val result = controller.doReport(request)

      status(result) should be(200)

      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("report-confirmation") should not be null
    }

    "return 200 and a valid json for a valid request and js is enabled" in new ProblemReportsControllerApplication {

      when(hmrcDeskproConnector.createTicket(meq("John Densmore"), meq("name@mail.com"), meq("Support Request"), meq(controller.problemMessage("Some Action", "Some Error")), meq("/contact/problem_reports"), meq(true), any[Request[AnyRef]]())(Matchers.any(classOf[HeaderCarrier]))).thenReturn(Future.successful(Some(TicketId(123))))

      val result = controller.doReport(generateRequest())

      status(result) should be(200)

      val message = contentAsJson(result).\("message").as[String]
      contentAsJson(result).\("status").as[String] shouldBe "OK"
      message should include("<h2 id=\"feedback-thank-you-header\">Thank you</h2>")
      message should include("the team will get back to you within 2 working days.")
    }

    "return 200 and a valid html page for invalid input and js is not enabled" in new ProblemReportsControllerApplication {

      val result = controller.doReport(generateInvalidRequest(javascriptEnabled = false))

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

    "fail if the email has invalid syntax (for DeskPRO)" in new ProblemReportsControllerApplication {
      val submit = controller.doReport(generateRequest(javascriptEnabled = false, email = "a@a"))
      val page = Jsoup.parse(contentAsString(submit))

      status(submit) shouldBe 200
      verifyZeroInteractions(hmrcDeskproConnector)

      page.getElementById("report-confirmation-no-data") should not be null
    }

  }

}

class ProblemReportsControllerApplication extends WithApplication(FakeApplication()) with MockitoSugar {
  val auditConnector: AuditConnector = mock[AuditConnector]
  val hmrcDeskproConnector: HmrcDeskproConnector = mock[HmrcDeskproConnector]
  implicit val authConnector: AuthConnector = mock[AuthConnector]
  val controller = new ProblemReportsController(auditConnector, hmrcDeskproConnector)

  val user = {
    val root = PayeRoot("nino", "mr", "John", None, "Densmore", "JD", "DOB", Map.empty, Map.empty, Map.empty)
    Some(User("123", Authority("/auth/oid/123", Credentials(), Accounts(), None, None, CreationAndLastModifiedDetail()), RegimeRoots(Some(root))))
  }

  def generateRequest(javascriptEnabled: Boolean = true, email: String = "name@mail.com") = FakeRequest()
    .withHeaders(("referer", "/contact/problem_reports"), ("User-Agent", "iAmAUserAgent"))
    .withFormUrlEncodedBody("report-name" -> "John Densmore", "report-email" -> email, "report-telephone" -> "012345678",
      "report-action" -> "Some Action", "report-error" -> "Some Error", "isJavascript" -> javascriptEnabled.toString)

  def generateInvalidRequest(javascriptEnabled: Boolean = true) = FakeRequest()
    .withHeaders(("referer", "/contact/problem_reports"), ("User-Agent", "iAmAUserAgent"))
    .withFormUrlEncodedBody("isJavascript" -> javascriptEnabled.toString)

  val request = generateRequest(javascriptEnabled = false)

  def hrmcConnectorWillReturnTheTicketId = {
    when(hmrcDeskproConnector.createTicket(meq("John Densmore"), meq("name@mail.com"), meq("Support Request"), meq(controller.problemMessage("Some Action", "Some Error")), meq("/contact/problem_reports"), meq(false), any[Request[AnyRef]]())(Matchers.any(classOf[HeaderCarrier]))).thenReturn(Future.successful(Some(TicketId(123))))
  }
}
