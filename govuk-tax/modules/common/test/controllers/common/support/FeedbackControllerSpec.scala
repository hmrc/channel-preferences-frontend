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
import uk.gov.hmrc.common.microservice.auth.domain.Credentials
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.common.microservice.domain.RegimeRoots
import uk.gov.hmrc.common.microservice.paye.domain.PayeRoot
import uk.gov.hmrc.common.microservice.auth.domain.Accounts
import play.api.test.FakeApplication
import uk.gov.hmrc.common.microservice.auth.domain.Authority
import scala.Some
import org.apache.commons.lang.StringUtils
import play.api.mvc.{Request, SimpleResult}
import scala.concurrent.Future
import org.scalatest.Matchers
import uk.gov.hmrc.common.microservice.deskpro.domain.TicketId
import controllers.common.actions.HeaderCarrier

class FeedbackControllerSpec extends BaseSpec {

  "Feedback controller" should {

    "render feedback form" in new FeedbackControllerApplication {

      val result = controller.renderForm(user, FakeRequest())
      status(result) shouldBe 200

    }

    "display error when rating is not selected" in new FeedbackControllerApplication {

      val result = controller.doSubmit(user, request(rating = ""))
      status(result) shouldBe 400
      formContainsError(result, "Please rate your experience")

    }

    "display error when submitting invalid rating value" in new FeedbackControllerApplication {

      val result = controller.doSubmit(user, request(rating = "pants"))
      status(result) shouldBe 400
      formContainsError(result, "Please select a valid experience rating")

    }

    "display error when name is empty" in new FeedbackControllerApplication {

      val result = controller.doSubmit(user, request(name = ""))
      status(result) shouldBe 400
      formContainsError(result, "Please provide your name")

    }

    "display error when name is too long" in new FeedbackControllerApplication {

      val result = controller.doSubmit(user, request(name = stringOfLength(71)))
      status(result) shouldBe 400
      formContainsError(result, "Your name cannot be longer than 70 characters")
    }

    "submit feedback with maximum name length" in new FeedbackControllerApplication {
      val result = controller.doSubmit(user, request(name = stringOfLength(70)))
      status(result) shouldBe 303
    }

    "display error when email is empty" in new FeedbackControllerApplication {

      val result = controller.doSubmit(user, request(email = ""))
      status(result) shouldBe 400
      formContainsError(result, "Enter a valid email address")

    }

    "display error when email is too long" in new FeedbackControllerApplication {

      val result = controller.doSubmit(user, request(email = s"${stringOfLength(317)}@a.a"))
      status(result) shouldBe 400
      formContainsError(result, "The email cannot be longer than 320 characters")
    }

    "submit feedback with maximum email length" in new FeedbackControllerApplication {
      val result = controller.doSubmit(user, request(email = s"${stringOfLength(316)}@a.a"))
      status(result) shouldBe 303
    }

    "display error when email address is not valid" in new FeedbackControllerApplication {

      val result = controller.doSubmit(user, request(email = "this is not an email address"))
      status(result) shouldBe 400
      formContainsError(result, "Enter a valid email address")

    }

    "display error when comments are empty" in new FeedbackControllerApplication {

      val result = controller.doSubmit(user, request(comments = ""))
      status(result) shouldBe 400
      formContainsError(result, "Please provide details")

    }

    "display error when comments are too verbose" in new FeedbackControllerApplication {

      val result = controller.doSubmit(user, request(comments = stringOfLength(2001)))
      status(result) shouldBe 400
      formContainsError(result, "The comment cannot be longer than 2000 characters")
    }

    "submit feedback with maximum comments length" in new FeedbackControllerApplication {
      val result = controller.doSubmit(user, request(comments = stringOfLength(2000)))
      status(result) shouldBe 303
    }

    "submits the feedback" in new FeedbackControllerApplication {

      val result = controller.redirectToConfirmationPage(Future.successful(Some(TicketId(123))))(user, request())
      status(result) shouldBe 303
      result.header.headers("Location") shouldBe "/beta-feedback/thanks"
    }

    "renders confirmation page" in new FeedbackControllerApplication {
      val result = controller.doThanks(user, request())
      status(result) shouldBe 200
      contentAsString(result) should include("Your feedback will be reviewed by our customer support team")
    }


  }


}

class FeedbackControllerApplication extends WithApplication(FakeApplication()) with MockitoSugar with Matchers {
  def stringOfLength(length: Int) = StringUtils.repeat("A", length)

  val deskProConnector = new HmrcDeskproConnector{
    override def createFeedback(name: String, email: String, rating: String, subject: String, message: String, referrer: String, isJavascript: Boolean, request: Request[AnyRef], user: Option[User])(implicit hc: HeaderCarrier): Future[Option[TicketId]] = {
      Future.successful(Some(TicketId(123)))
    }
  }
  val controller = new FeedbackController(mock[AuditConnector], deskProConnector)(mock[AuthConnector])

  val user = {
    val root = PayeRoot("nino", "mr", "John", None, "Densmore", "JD", "DOB", Map.empty, Map.empty, Map.empty)
    User("123", Authority("/auth/oid/123", Credentials(), Accounts(), None, None, CreationAndLastModifiedDetail()), RegimeRoots(Some(root)))
  }

  def request(rating: String = "Good",
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
