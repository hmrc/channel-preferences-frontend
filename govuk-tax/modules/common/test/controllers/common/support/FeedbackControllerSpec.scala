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
import org.mockito.Matchers.{eq => meq, any}
import uk.gov.hmrc.common.microservice.ct.domain.CtRoot
import controllers.domain.AuthorityUtils._
import uk.gov.hmrc.common.microservice.paye.domain.PayeRoot
import uk.gov.hmrc.common.microservice.auth.domain.Accounts
import uk.gov.hmrc.common.microservice.auth.domain.Authority
import scala.Some
import play.api.mvc.SimpleResult
import uk.gov.hmrc.domain.{SaUtr, CtUtr}
import uk.gov.hmrc.common.microservice.auth.domain.Credentials
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.common.microservice.domain.RegimeRoots
import uk.gov.hmrc.common.microservice.deskpro.domain.TicketId
import uk.gov.hmrc.common.microservice.preferences.{SaPreference, PreferencesConnector}
import uk.gov.hmrc.common.microservice.sa.domain.SaRoot
import org.jsoup.nodes.Document

class FeedbackControllerSpec extends BaseSpec {

  "Rendering the feedback form" should {

    "work for an authenticated user" in new FeedbackControllerApplication {
      val result = controller.authenticatedFeedback(user, FakeRequest())
      status(result) shouldBe 200
      val doc = Jsoup.parse(contentAsString(result))
      formActionIn(doc) shouldBe authenticatedSubmitUri
      navigationShouldNotBeVisibleIn(doc)
    }

    "work for an unauthenticated user" in new FeedbackControllerApplication {
      val result = controller.unauthenticatedFeedback(FakeRequest())
      status(result) shouldBe 200
      val doc = Jsoup.parse(contentAsString(result))
      formActionIn(doc) shouldBe unauthenticatedSubmitUri
      navigationShouldNotBeVisibleIn(doc)
    }
  }

  "Submitting correct data in the feedback form" should {

    "work for authenticated feedback" in new FeedbackControllerApplication {
      val ticket = Some(TicketId(123))
      when(ticketCache.stashTicket(meq(ticket), meq("FeedbackForm"))(any[HeaderCarrier])).thenReturn(Future.successful("stored"))

      val result = controller.redirectToConfirmationPage(Future.successful(ticket), someUser)(request())

      status(result) shouldBe 303
      result.header.headers("Location") shouldBe "/beta-feedback/thanks"
      verify(ticketCache).stashTicket(meq(ticket), meq("FeedbackForm"))(any[HeaderCarrier])
    }

    "work for unauthenticated feedback" in new FeedbackControllerApplication {
      val ticket = Some(TicketId(123))
      when(ticketCache.stashTicket(meq(ticket), meq("FeedbackForm"))(any[HeaderCarrier])).thenReturn(Future.successful("stored"))

      val result = controller.redirectToConfirmationPage(Future.successful(ticket), None)(request())

      status(result) shouldBe 303
      result.header.headers("Location") shouldBe "/beta-feedback/thanks-unauthenticated"
      verify(ticketCache).stashTicket(meq(ticket), meq("FeedbackForm"))(any[HeaderCarrier])
    }

    "allow feedback with comments of max length" in new FeedbackControllerApplication {
      val result = controller.doSubmit(someUser)(request(comments = stringOfLength(2000)))
      status(result) shouldBe 303
    }

    "allow feedback with maximum email length" in new FeedbackControllerApplication {
      val result = controller.doSubmit(someUser)(request(email = s"${stringOfLength(251)}@a.a"))
      status(result) shouldBe 303
    }

    "allow feedback with maximum name length" in new FeedbackControllerApplication {
      val result = controller.doSubmit(someUser)(request(name = stringOfLength(70)))
      status(result) shouldBe 303
    }
  }

  "Rendering of feedback form when incorrect data is submitted" should {

    "work for an authenticated user" in new FeedbackControllerApplication {
      val result = controller.doSubmit(Some(user))(FakeRequest())
      status(result) shouldBe 400
      val doc = Jsoup.parse(contentAsString(result))
      formActionIn(doc) shouldBe authenticatedSubmitUri
      navigationShouldNotBeVisibleIn(doc)
    }

    "work navigation for unauthenticated user" in new FeedbackControllerApplication {
      val result = controller.doSubmit(None)(FakeRequest())
      status(result) shouldBe 400
      val doc = Jsoup.parse(contentAsString(result))
      formActionIn(doc) shouldBe unauthenticatedSubmitUri
      navigationShouldNotBeVisibleIn(doc)
    }
  }

  "Errors on the feedback form" should {

    "show when rating is not selected" in new FeedbackControllerApplication {
      expectErrorFor(request(rating = ""), message = "Tell us what you think of the service.")
    }

    "show when rating is not selected at all" in new FeedbackControllerApplication {
      expectErrorFor(
        FakeRequest().withFormUrlEncodedBody(
          "feedback-name" -> "name",
          "feedback-email" -> "email@foo.com",
          "feedback-comments" -> "unrateable",
          "referer" -> "referer",
          "isJavascript" -> "false"),
        message = "Tell us what you think of the service."
      )
    }

    "show when submitting invalid rating value" in new FeedbackControllerApplication {
      expectErrorFor(request(rating = "dude, you suck."), message = "Please select a valid experience rating")
    }

    "show when name is empty" in new FeedbackControllerApplication {
      expectErrorFor(request(name = ""), message = "Please provide your name.")
    }

    "show when name is too long" in new FeedbackControllerApplication {
      expectErrorFor(request(name = stringOfLength(71)), message = "Your name cannot be longer than 70 characters")
    }

    "show when email is empty" in new FeedbackControllerApplication {
      expectErrorFor(request(email = ""), message = "Enter a valid email address.")
    }

    "show when email is too long" in new FeedbackControllerApplication {
      expectErrorFor(request(email = s"${stringOfLength(256)}@a.a"), message = "The email cannot be longer than 255 characters")
    }

    "show when email address is not valid" in new FeedbackControllerApplication {
      expectErrorFor(request(email = "this is not an email address"), message = "Enter a valid email address.")
    }

    "show when email address is not valid for DeskPRO" in new FeedbackControllerApplication {
      expectErrorFor(request(email = "a@a"), message = "Enter a valid email address.")
    }

    "show when comments are empty" in new FeedbackControllerApplication {
      expectErrorFor(request(comments = ""), message = "Enter your comments.")
    }

    "show when comments are too verbose" in new FeedbackControllerApplication {
      expectErrorFor(request(comments = stringOfLength(2001)), message = "The comment cannot be longer than 2000 characters")
    }
  }


  "The confirmation page" should {
    "render" in new FeedbackControllerApplication {
      when(ticketCache.popTicket(meq("FeedbackForm"))(any[HeaderCarrier])).thenReturn(Future.successful("321"))
      val result = controller.doThanks(someUser, request())
      val page = Jsoup.parse(contentAsString(result))

      status(result) shouldBe 200
      contentAsString(result) should include("Your feedback has been received")
      page.getElementById("ticketId").attr("value") shouldBe "321"
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

  val prefsConnector = mock[PreferencesConnector]

  val controller = new FeedbackController(mock[AuditConnector], deskProConnector, ticketCache, prefsConnector)(mock[AuthConnector])

  val payeUser = {
    val root = PayeRoot("nino", "mr", "John", None, "Densmore", "JD", "DOB", Map.empty, Map.empty, Map.empty)
    Some(User("123", Authority("/auth/oid/123", Credentials(), Accounts(), None, None, CreationAndLastModifiedDetail()), RegimeRoots(Some(root))))
  }

  val user = User(
    userId = "userId", 
    userAuthority = ctAuthority("userId", "ct-utr"), 
    nameFromGovernmentGateway = Some("Ciccio"),
    regimes = RegimeRoots(ct = Some(CtRoot(CtUtr("ct-utr"), Map.empty[String, String]))),
    decryptedToken = None)
  val someUser = Some(user)

  val nonSaUser = user
  val saUser = User(
    userId = "userId",
    userAuthority = saAuthority("userId", "sa-utr"),
    nameFromGovernmentGateway = Some("Ciccio"),
    regimes = RegimeRoots(sa = Some(SaRoot(SaUtr("sa-utr"), Map.empty[String, String]))),
    decryptedToken = None)

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

  val authenticatedSubmitUri = "/beta-feedback/submit"
  val unauthenticatedSubmitUri = "/beta-feedback/submit-unauthenticated"

  def whenUserPrefsAre(prefs: Option[SaPreference]) = when(prefsConnector.getPreferences(meq(saUser.getSaUtr))(any())).thenReturn(Future.successful(prefs))

  def formActionIn(doc: Document): String = doc.getElementById("feedback-form").attr("action")
  
  def navigationShouldNotBeVisibleIn(doc: Document) {
    doc.getElementById("proposition-menu") shouldBe null
  }

  def expectErrorFor(request: Request[AnyRef], message: String) {
    val result = controller.doSubmit(someUser)(request)
    status(result) shouldBe 400
    formContainsError(result, message)
  }
}
