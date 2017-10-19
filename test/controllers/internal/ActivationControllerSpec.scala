package controllers.internal

import _root_.connectors._
import controllers.AuthorityUtils._
import helpers.TestFixtures
import org.jsoup.Jsoup
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.frontend.auth._
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.test.UnitSpec
import play.api.mvc.Results.{NotFound, Ok, PreconditionFailed}
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

abstract class ActivationControllerSetup extends MockitoSugar {
  val request = FakeRequest()
  val mockAuthConnector = mock[AuthConnector]
  val mockAuditConnector = mock[AuditConnector]
  val user = AuthContext(authority = emptyAuthority("userId"), nameFromSession = Some("Ciccio"), governmentGatewayToken = None)

  val mockEntityResolverConnector : EntityResolverConnector = {
    val entityResolverMock = mock[EntityResolverConnector]
    when(entityResolverMock.getPreferencesStatus(any())(any())).thenReturn(Future.successful(Right[Int,PreferenceStatus](PreferenceNotFound(None))))
    entityResolverMock
  }

  val controller = new ActivationController {
    override def entityResolverConnector: EntityResolverConnector = mockEntityResolverConnector

    override val hostUrl: String = ""

    override protected implicit def authConnector: AuthConnector = mockAuthConnector
    override def authenticated = AuthenticatedBy(TestAuthenticationProvider, pageVisibility = GGConfidence)

  }

  object TestAuthenticationProvider extends AuthenticationProvider {

    override val id = "TST"

    def login = "/login"

    def redirectToLogin(implicit request: Request[_]) = Future.successful(Results.Redirect(login))

    def handleNotAuthenticated(implicit request: Request[_]) = {
      case _ => Future.successful(Left(user))
    }
  }
}
class ActivationControllerSpec extends UnitSpec with OneAppPerSuite {


  "The Activation with a token" should {

    "fail when not supplied with a mtdfbit service" in new ActivationControllerSetup {
      when(mockEntityResolverConnector.getPreferencesStatusByToken(any(),any(),any())(any())).thenReturn(Future.successful(Left(NOT_FOUND)))
      val res: Future[Result] = controller.preferencesStatusBySvc("svc", "token",TestFixtures.sampleHostContext)(request)
      status(res) shouldBe NotFound.header.status
    }

    "succeed when the service is mtdfbit" in new ActivationControllerSetup {
      val email = EmailPreference("test@test.com", false, false, false,None)
      when(mockEntityResolverConnector.getPreferencesStatusByToken(any(),any(),any())(any())).thenReturn(Future.successful(Right(PreferenceNotFound(Some(email)))))
      val res: Future[Result] = controller.preferencesStatusBySvc("mtdfbit", "token",TestFixtures.sampleHostContext)(request)
      status(res) shouldBe PreconditionFailed.header.status
      val document = Jsoup.parse(contentAsString(res))
      document.getElementsByTag("body").first().html() should startWith("""{"redirectUserTo":"/paperless/choose/cohort/mtdfbit/token?email=""")
    }

    "return a json body with optedIn set to true if preference is found and opted-in" in new ActivationControllerSetup {
      val email = EmailPreference("test@test.com", false, false, false,None)
      when(mockEntityResolverConnector.getPreferencesStatusByToken(any(),any(),any())(any())).thenReturn(Future.successful(Right(PreferenceFound(true, Some(email)))))
      val res: Future[Result] = controller.preferencesStatusBySvc("mtdfbit", "token",TestFixtures.sampleHostContext)(request)

      status(res) shouldBe Ok.header.status
      val document = Jsoup.parse(contentAsString(res))
      document.getElementsByTag("body").first().html() should include("""{"optedIn":true,"verifiedEmail":false}""")
    }

    "return a json body with optedIn set to false and a return sign up URL if preference is found and opted-out" in new ActivationControllerSetup {
      val email = EmailPreference("test@test.com", false, false, false,None)
      when(mockEntityResolverConnector.getPreferencesStatusByToken(any(),any(),any())(any())).thenReturn(Future.successful(Right(PreferenceFound(false, Some(email)))))
      val res: Future[Result] = controller.preferencesStatusBySvc("mtdfbit", "token",TestFixtures.sampleHostContext)(request)

      status(res) shouldBe Ok.header.status
      val document = Jsoup.parse(contentAsString(res))
      document.getElementsByTag("body").first().html() should startWith("""{"optedIn":false,"redirectUserTo":"/paperless/choose/cohort/mtdfbit/token?email=""")
    }
  }
}
