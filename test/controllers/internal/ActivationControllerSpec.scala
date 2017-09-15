package controllers.internal

import _root_.connectors._
import controllers.AuthorityUtils._
import helpers.TestFixtures
import org.jsoup.Jsoup

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.frontend.auth._
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.test.UnitSpec
import play.api.mvc.Results.{NotFound, PreconditionFailed}

import scala.concurrent.Future

abstract class ActivationControllerSetup extends MockitoSugar {
  val request = FakeRequest()
  val mockAuthConnector = mock[AuthConnector]
  val mockAuditConnector = mock[AuditConnector]
  val user = AuthContext(authority = emptyAuthority("userId"), nameFromSession = Some("Ciccio"), governmentGatewayToken = None)

  val controller = new ActivationController {
    override def entityResolverConnector: EntityResolverConnector = ???

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

    "fail when not supplied with a mtdfbit servicer" in new ActivationControllerSetup {
      val res: Future[Result] = controller.preferencesStatusMtd("svc", "token",TestFixtures.sampleHostContext)(request)
      status(res) shouldBe NotFound.header.status
    }

    "succeed when the service is mtdfbit" in new ActivationControllerSetup {
      val res: Future[Result] = controller.preferencesStatusMtd("mtdfbit", "token",TestFixtures.sampleHostContext)(request)
      status(res) shouldBe PreconditionFailed.header.status
      val document = Jsoup.parse(contentAsString(res))
      document.getElementsByTag("body").first().html() shouldBe """{"redirectUserTo":"/income-tax-subscription/"}"""
    }
  }
}
