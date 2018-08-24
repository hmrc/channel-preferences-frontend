package controllers.internal

import _root_.connectors._
import controllers.auth.AuthAction
import helpers.{MockAuthController, TestFixtures}
import org.jsoup.Jsoup
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import play.api.mvc.Results.{NotFound, Ok, PreconditionFailed}
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.{LegacyCredentials, ~}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future

abstract class ActivationControllerSetup extends MockitoSugar {
  val request = FakeRequest()

  val mockAuditConnector = mock[AuditConnector]

  val mockEntityResolverConnector: EntityResolverConnector = {
    val entityResolverMock = mock[EntityResolverConnector]
    when(entityResolverMock.getPreferencesStatus(any())(any())).thenReturn(Future.successful(Right[Int, PreferenceStatus](PreferenceNotFound(None))))
    entityResolverMock
  }

  protected class TestActivationController(stubbedRetrievalResult: Future[_]) extends ActivationController {

    override def authorise: AuthAction = new MockAuthController(None, None, None, None)

    override def entityResolverConnector: EntityResolverConnector = mockEntityResolverConnector

    override val hostUrl: String = ""
  }

  val retrievalResult: Future[~[LegacyCredentials, Enrolments]] = Future.successful(null)
  val controller = new TestActivationController(retrievalResult)

}

class ActivationControllerSpec extends UnitSpec with OneAppPerSuite {

  import scala.concurrent.ExecutionContext.Implicits.global

  "The Activation with an AuthContext" should {
    "return a json body with optedIn set to true if preference is found and opted-in and no alreadyOptedInUrl is present" in new ActivationControllerSetup {
      val email = EmailPreference("test@test.com", false, false, false, None)
      when(mockEntityResolverConnector.getPreferencesStatus(any())(any())).thenReturn(Future.successful(Right(PreferenceFound(true, Some(email)))))
      val res: Future[Result] = controller.preferencesStatus(TestFixtures.sampleHostContext)(request)
      status(res) shouldBe Ok.header.status
      val document = Jsoup.parse(contentAsString(res))
      document.getElementsByTag("body").first().html() should include("""{"optedIn":true,"verifiedEmail":false}""")
    }

    "redirect to the alreadyOptedInUrl if preference is found and opted-in and an alreadyOptedInUrl is present" in new ActivationControllerSetup {
      val email = EmailPreference("test@test.com", false, false, false, None)
      when(mockEntityResolverConnector.getPreferencesStatus(any())(any())).thenReturn(Future.successful(Right(PreferenceFound(true, Some(email)))))
      val res: Future[Result] = controller.preferencesStatus(TestFixtures.alreadyOptedInUrlHostContext)(request)

      status(res) shouldBe SEE_OTHER
      res.map { result =>
        result.header.headers should contain("Location")
        result.header.headers.get("Location") shouldBe TestFixtures.alreadyOptedInUrlHostContext.alreadyOptedInUrl.get
      }
    }

    "return a json body with optedIn set to false if preference is found and opted-in and no alreadyOptedInUrl is present" in new ActivationControllerSetup {
      val email = EmailPreference("test@test.com", false, false, false, None)
      when(mockEntityResolverConnector.getPreferencesStatus(any())(any())).thenReturn(Future.successful(Right(PreferenceFound(false, Some(email)))))
      val res: Future[Result] = controller.preferencesStatus(TestFixtures.sampleHostContext)(request)

      status(res) shouldBe Ok.header.status
      val document = Jsoup.parse(contentAsString(res))
      document.getElementsByTag("body").first().html() should include("""{"optedIn":false}""")
    }

    "return a json body with optedIn set to false if preference is found and opted-in and an alreadyOptedInUrl is present" in new ActivationControllerSetup {
      val email = EmailPreference("test@test.com", false, false, false, None)
      when(mockEntityResolverConnector.getPreferencesStatus(any())(any())).thenReturn(Future.successful(Right(PreferenceFound(false, Some(email)))))
      val res: Future[Result] = controller.preferencesStatus(TestFixtures.alreadyOptedInUrlHostContext)(request)

      status(res) shouldBe Ok.header.status
      val document = Jsoup.parse(contentAsString(res))
      document.getElementsByTag("body").first().html() should include("""{"optedIn":false}""")
    }

    "return PRECONDITION failed if no preferences are found and no alreadyOptedInUrl is present" in new ActivationControllerSetup {
      val email = EmailPreference("test@test.com", false, false, false, None)
      when(mockEntityResolverConnector.getPreferencesStatus(any())(any())).thenReturn(Future.successful(Right(PreferenceNotFound(Some(email)))))
      val res: Future[Result] = controller.preferencesStatus(TestFixtures.sampleHostContext)(request)

      status(res) shouldBe PRECONDITION_FAILED
      val document = Jsoup.parse(contentAsString(res))
      document.getElementsByTag("body").first().html() should startWith("""{"redirectUserTo":"/paperless/choose?email=""")
    }

    "return PRECONDITION failed if no preferences are found and an alreadyOptedInUrl is present" in new ActivationControllerSetup {
      val email = EmailPreference("test@test.com", false, false, false, None)
      when(mockEntityResolverConnector.getPreferencesStatus(any())(any())).thenReturn(Future.successful(Right(PreferenceNotFound(Some(email)))))
      val res: Future[Result] = controller.preferencesStatus(TestFixtures.alreadyOptedInUrlHostContext)(request)

      status(res) shouldBe PRECONDITION_FAILED
      val document = Jsoup.parse(contentAsString(res))
      document.getElementsByTag("body").first().html() should startWith("""{"redirectUserTo":"/paperless/choose?email=""")
    }
  }

  "The Activation with a token" should {

    "fail when not supplied with a mtdfbit service" in new ActivationControllerSetup {
      when(mockEntityResolverConnector.getPreferencesStatusByToken(any(), any(), any())(any())).thenReturn(Future.successful(Left(NOT_FOUND)))
      val res: Future[Result] = controller.preferencesStatusBySvc("svc", "token", TestFixtures.sampleHostContext)(request)
      status(res) shouldBe NotFound.header.status
    }

    "succeed when the service is mtdfbit" in new ActivationControllerSetup {
      val email = EmailPreference("test@test.com", false, false, false, None)
      when(mockEntityResolverConnector.getPreferencesStatusByToken(any(), any(), any())(any())).thenReturn(Future.successful(Right(PreferenceNotFound(Some(email)))))
      val res: Future[Result] = controller.preferencesStatusBySvc("mtdfbit", "token", TestFixtures.sampleHostContext)(request)
      status(res) shouldBe PreconditionFailed.header.status
      val document = Jsoup.parse(contentAsString(res))
      document.getElementsByTag("body").first().html() should startWith("""{"redirectUserTo":"/paperless/choose/cohort/mtdfbit/token?email=""")
    }

    "return a json body with optedIn set to true if preference is found and opted-in and no alreadyOptedInUrl present" in new ActivationControllerSetup {
      val email = EmailPreference("test@test.com", false, false, false, None)
      when(mockEntityResolverConnector.getPreferencesStatusByToken(any(), any(), any())(any())).thenReturn(Future.successful(Right(PreferenceFound(true, Some(email)))))
      val res: Future[Result] = controller.preferencesStatusBySvc("mtdfbit", "token", TestFixtures.sampleHostContext)(request)

      status(res) shouldBe Ok.header.status
      val document = Jsoup.parse(contentAsString(res))
      document.getElementsByTag("body").first().html() should include("""{"optedIn":true,"verifiedEmail":false}""")
    }

    "return a json body with optedIn set to true if preference is found and opted-in and an alreadyOptedInUrl present" in new ActivationControllerSetup {
      val email = EmailPreference("test@test.com", false, false, false, None)
      when(mockEntityResolverConnector.getPreferencesStatusByToken(any(), any(), any())(any())).thenReturn(Future.successful(Right(PreferenceFound(true, Some(email)))))
      val res: Future[Result] = controller.preferencesStatusBySvc("mtdfbit", "token", TestFixtures.alreadyOptedInUrlHostContext)(request)

      status(res) shouldBe Ok.header.status
      val document = Jsoup.parse(contentAsString(res))
      document.getElementsByTag("body").first().html() should include("""{"optedIn":true,"verifiedEmail":false}""")
    }

    "return a json body with optedIn set to false and a return sign up URL if preference is found and opted-out" in new ActivationControllerSetup {
      val email = EmailPreference("test@test.com", false, false, false, None)
      when(mockEntityResolverConnector.getPreferencesStatusByToken(any(), any(), any())(any())).thenReturn(Future.successful(Right(PreferenceFound(false, Some(email)))))
      val res: Future[Result] = controller.preferencesStatusBySvc("mtdfbit", "token", TestFixtures.sampleHostContext)(request)

      status(res) shouldBe Ok.header.status
      val document = Jsoup.parse(contentAsString(res))
      document.getElementsByTag("body").first().html() should startWith("""{"optedIn":false,"redirectUserTo":"/paperless/choose/cohort/mtdfbit/token?email=""")
    }
  }
}
