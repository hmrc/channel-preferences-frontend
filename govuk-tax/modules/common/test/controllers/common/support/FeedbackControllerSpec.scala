package controllers.common.support

import uk.gov.hmrc.common.BaseSpec
import play.api.test.{FakeRequest, WithApplication}
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.common.microservice.audit.AuditConnector
import uk.gov.hmrc.common.microservice.deskpro.HmrcDeskproConnector
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import uk.gov.hmrc.common.microservice.auth.domain.CreationAndLastModifiedDetail
import play.api.test.Helpers._
import org.jsoup.Jsoup
import org.apache.commons.lang.StringUtils
import play.api.mvc.Request
import scala.concurrent.Future
import controllers.common.actions.HeaderCarrier
import org.mockito.Mockito._
import uk.gov.hmrc.common.microservice.paye.domain.PayeRoot
import uk.gov.hmrc.common.microservice.auth.domain.Accounts
import uk.gov.hmrc.common.microservice.auth.domain.Authority
import scala.Some
import play.api.mvc.SimpleResult
import uk.gov.hmrc.common.microservice.auth.domain.Credentials
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.common.microservice.domain.RegimeRoots
import uk.gov.hmrc.common.microservice.deskpro.domain.TicketId
import org.mockito.Matchers.{eq => meq, any}

class FeedbackControllerSpec extends BaseSpec {

  "Feedback controller" should {

    "render feedback form for authenticated user" in new FeedbackControllerApplication {

      val result = controller.authenticatedFeedback(user.get, FakeRequest())
      status(result) shouldBe 200
      val doc = Jsoup.parse(contentAsString(result))
      doc.getElementById("feedback-form").attr("action") shouldBe "/beta-feedback/submit"
    }

    "render feedback form for unauthenticated user" in new FeedbackControllerApplication {

      val result = controller.unauthenticatedFeedback(FakeRequest())
      status(result) shouldBe 200
      val doc = Jsoup.parse(contentAsString(result))
      doc.getElementById("feedback-form").attr("action") shouldBe "/beta-feedback/submit-unauthenticated"
    }

    "display error when rating is not selected" in new FeedbackControllerApplication {

      val result = controller.doSubmit(user)(request(rating = ""))
      status(result) shouldBe 400
      formContainsError(result, "Tell us what you think of the service.")
    }

    "display error when rating is not selected at all" in new FeedbackControllerApplication {

      val result = controller.doSubmit(user)(FakeRequest().withFormUrlEncodedBody(
        "feedback-name" -> "name",
        "feedback-email" -> "email@foo.com",
        "feedback-comments" -> "unrateable",
        "referer" -> "referer",
        "isJavascript" -> "false"))
      status(result) shouldBe 400
      formContainsError(result, "Tell us what you think of the service.")
    }

    "display error when submitting invalid rating value" in new FeedbackControllerApplication {

      val result = controller.doSubmit(user)(request(rating = "pants"))
      status(result) shouldBe 400
      formContainsError(result, "Please select a valid experience rating")
    }

    "display error when name is empty" in new FeedbackControllerApplication {

      val result = controller.doSubmit(user)(request(name = ""))
      status(result) shouldBe 400
      formContainsError(result, "Please provide your name")
    }

    "display error when name is too long" in new FeedbackControllerApplication {

      val result = controller.doSubmit(user)(request(name = stringOfLength(71)))
      status(result) shouldBe 400
      formContainsError(result, "Your name cannot be longer than 70 characters")
    }

    "submit feedback with maximum name length" in new FeedbackControllerApplication {
      val result = controller.doSubmit(user)(request(name = stringOfLength(70)))
      status(result) shouldBe 303
    }

    "display error when email is empty" in new FeedbackControllerApplication {

      val result = controller.doSubmit(user)(request(email = ""))
      status(result) shouldBe 400
      formContainsError(result, "Enter a valid email address")
    }

    "display error when email is too long" in new FeedbackControllerApplication {

      val result = controller.doSubmit(user)(request(email = s"${stringOfLength(317)}@a.a"))
      status(result) shouldBe 400
      formContainsError(result, "The email cannot be longer than 320 characters")
    }

    "submit feedback with maximum email length" in new FeedbackControllerApplication {
      val result = controller.doSubmit(user)(request(email = s"${stringOfLength(316)}@a.a"))
      status(result) shouldBe 303
    }

    "display error when email address is not valid" in new FeedbackControllerApplication {

      val result = controller.doSubmit(user)(request(email = "this is not an email address"))
      status(result) shouldBe 400
      formContainsError(result, "Enter a valid email address")

    }

    "display error when comments are empty" in new FeedbackControllerApplication {

      val result = controller.doSubmit(user)(request(comments = ""))
      status(result) shouldBe 400
      formContainsError(result, "Enter your comments")
    }

    "display error when comments are too verbose" in new FeedbackControllerApplication {

      val result = controller.doSubmit(user)(request(comments = stringOfLength(2001)))
      status(result) shouldBe 400
      formContainsError(result, "The comment cannot be longer than 2000 characters")
    }

    "submit feedback with maximum comments length" in new FeedbackControllerApplication {
      val result = controller.doSubmit(user)(request(comments = stringOfLength(2000)))
      status(result) shouldBe 303
    }

    "submit the authenticated feedback" in new FeedbackControllerApplication {
      val ticket = Some(TicketId(123))
      when(ticketCache.stashTicket(meq(ticket), meq("FeedbackForm"))(any[HeaderCarrier])).thenReturn(Future.successful("stored"))

      val result = controller.redirectToConfirmationPage(Future.successful(ticket), user)(request())

      status(result) shouldBe 303
      result.header.headers("Location") shouldBe "/beta-feedback/thanks"
      verify(ticketCache).stashTicket(meq(ticket), meq("FeedbackForm"))(any[HeaderCarrier])
    }

    "render confirmation page" in new FeedbackControllerApplication {
      when(ticketCache.popTicket(meq("FeedbackForm"))(any[HeaderCarrier])).thenReturn(Future.successful("321"))
      val result = controller.doThanks(user, request())
      val page = Jsoup.parse(contentAsString(result))

      status(result) shouldBe 200
      contentAsString(result) should include("Your feedback will be reviewed by our customer support team")
      page.getElementById("ticketId").attr("value") shouldBe "321"
    }

    "submit the unauthenticated feedback" in new FeedbackControllerApplication {
      val ticket = Some(TicketId(123))
      when(ticketCache.stashTicket(meq(ticket), meq("FeedbackForm"))(any[HeaderCarrier])).thenReturn(Future.successful("stored"))

      val result = controller.redirectToConfirmationPage(Future.successful(ticket), None)(request())

      status(result) shouldBe 303
      result.header.headers("Location") shouldBe "/beta-feedback/thanks-unauthenticated"
      verify(ticketCache).stashTicket(meq(ticket), meq("FeedbackForm"))(any[HeaderCarrier])
    }
  }
}

class FeedbackControllerApplication extends WithApplication with MockitoSugar with org.scalatest.Matchers {
  lazy val ticketCache = mock[TicketCache]

  def stringOfLength(length: Int) = StringUtils.repeat("A", length)

  val deskProConnector = new HmrcDeskproConnector {
    override def createFeedback(name: String, email: String, rating: String, subject: String, message: String, referrer: String, isJavascript: Boolean, request: Request[AnyRef], user: Option[User])(implicit hc: HeaderCarrier): Future[Option[TicketId]] = {
      Future.successful(Some(TicketId(123)))
    }
  }

  val controller = new FeedbackController(mock[AuditConnector], deskProConnector, ticketCache)(mock[AuthConnector])

  val user = {
    val root = PayeRoot("nino", "mr", "John", None, "Densmore", "JD", "DOB", Map.empty, Map.empty, Map.empty)
    Some(User("123", Authority("/auth/oid/123", Credentials(), Accounts(), None, None, CreationAndLastModifiedDetail()), RegimeRoots(Some(root))))
  }

  def request(rating: String = "3",
              name: String = "Tom Smith",
              email: String = "tom.smith@gmail.com",
              comments: String = "I really enjoyed this experience. I do not have better things to do than sending feedback.") =
    FakeRequest().withFormUrlEncodedBody(
      "feedback-rating" -> rating,
      "feedback-name" -> name,
      "feedback-email" -> email,
      "feedback-comments" -> comments,
      "referer" -> "referer",
      "isJavascript" -> "false")


  def formContainsError(result: Future[SimpleResult], error: String) {
    val doc = Jsoup.parse(contentAsString(result))
    doc.getElementById("feedback-form").text() should include(error)
  }
}
