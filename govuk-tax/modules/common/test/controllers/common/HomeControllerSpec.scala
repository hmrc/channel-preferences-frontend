package controllers.common

import org.scalatest.mock.MockitoSugar
import java.util.UUID
import uk.gov.hmrc.common.BaseSpec
import play.api.test.WithApplication
import play.api.test.Helpers._
import play.api.mvc.{Session, Cookies}
import controllers.common.{routes => commonRoutes}
import uk.gov.hmrc.common.microservice.sa.domain.SaRoot
import uk.gov.hmrc.common.microservice.vat.domain.VatRoot
import uk.gov.hmrc.common.microservice.ct.domain.CtRoot
import uk.gov.hmrc.common.microservice.epaye.domain.EpayeRoot
import controllers.domain.AuthorityUtils._
import uk.gov.hmrc.common.microservice.paye.domain.PayeRoot
import uk.gov.hmrc.common.microservice.auth.domain.Authority
import scala.Some
import play.api.mvc.SimpleResult
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.common.microservice.domain.RegimeRoots
import play.api.test.FakeApplication

class HomeControllerSpec extends BaseSpec with MockitoSugar with CookieEncryption {

  private abstract class WithSetup extends WithApplication(FakeApplication()) {
    val controller = new HomeController()
  }

  "Calling redirectToLoginPage" should {

    "redirect to the login page and delete the session" in new WithSetup {
      val result = controller.redirectToLoginPage
      assertRedirectedToLoginWithNewSession(result)
    }
  }

  "Calling redirectToHomepage" should {

    "end the session and redirect to the login page if the user has no regimes and is not a Government Gateway user" in new WithSetup {
      val user = makeUser(emptyAuthority("userId"))
      val result = controller.redirectToHomepage(user, sessionFor(user))
      assertRedirectedToLoginWithNewSession(result)
    }

    "redirect to the PAYE homepage if the user is in PAYE (only) and has no redirect in the session" in new WithSetup {
      val user = makeUser(payeAuthority("userId","AB123456C"))
      val result = controller.redirectToHomepage(user, sessionFor(user))
      assertRedirected(result, FrontEndRedirect.payeHome)
    }

    "redirect to the page in the session if the user is in PAYE (only) and has a redirect in the session" in new WithSetup {
      val user = makeUser(payeAuthority("userId","AB123456C"))
      val result = controller.redirectToHomepage(user, sessionFor(user, Some("/some/page")))
      assertRedirected(result, "/some/page")
    }

    "redirect to the business tax homepage if the user is in SA (only)" in new WithSetup {

      val user = makeUser(saAuthority("userId", "1234543210"))
      val result = controller.redirectToHomepage(user, sessionFor(user))
      assertRedirected(result, FrontEndRedirect.businessTaxHome)
    }

    "redirect to the business tax homepage if the user is in VAT (only)" in new WithSetup {
      val user = makeUser(vatAuthority("userId", "234543210"))
      val result = controller.redirectToHomepage(user, sessionFor(user))
      assertRedirected(result, FrontEndRedirect.businessTaxHome)
    }

    "redirect to the business tax homepage if the user is in CT (only)" in new WithSetup {
      val user = makeUser( ctAuthority("userId", "1234543210"))
      val result = controller.redirectToHomepage(user, sessionFor(user))
      assertRedirected(result, FrontEndRedirect.businessTaxHome)
    }

    "redirect to the business tax homepage if the user is in EPAYE (only)" in new WithSetup {
      val user = makeUser(epayeAuthority("userId", "some/epaye"))
      val result = controller.redirectToHomepage(user, sessionFor(user))
      assertRedirected(result, FrontEndRedirect.businessTaxHome)
    }

    "redirect to the business tax homepage if the user has a combination of business tax enrolments" in new WithSetup {

      val user = makeUser(allBizTaxAuthority("userId", "1234543210", "1234543210", "234543210", "abc/123"))
      val result = controller.redirectToHomepage(user, sessionFor(user))
      assertRedirected(result, FrontEndRedirect.businessTaxHome)
    }

    "redirect to the business tax homepage if the user has no enrolments, but is a Government Gateway user" in new WithSetup {
      val user = makeUser(emptyAuthority("userId"), Some("Geoff Fisher"))
      val result = controller.redirectToHomepage(user, sessionFor(user))
      assertRedirected(result, FrontEndRedirect.businessTaxHome)
    }
  }

  private def makeUser(authority: Authority, nameFromGovernmentGateway: Option[String] = None) = {

    val governmentGatewayToken = nameFromGovernmentGateway.map(_ => "<token>Geoff Fisher</token>")

    User(
      userId = "/auth/oid/userId",
      userAuthority = authority,
      regimes = RegimeRoots(
        paye = authority.accounts.paye.map(_ => mock[PayeRoot]),
        sa = authority.accounts.sa.map(_ => mock[SaRoot]),
        ct = authority.accounts.ct.map(_ => mock[CtRoot]),
        vat = authority.accounts.vat.map(_ => mock[VatRoot]),
        epaye = authority.accounts.epaye.map(_ => mock[EpayeRoot])
      ),
      nameFromGovernmentGateway = nameFromGovernmentGateway,
      decryptedToken = governmentGatewayToken
    )
  }

  private def sessionFor(user: User, redirectUrlForPaye: Option[String] = None): Session = {

    val session = Map(
      "sessionId" -> Some(encrypt(s"session-${UUID.randomUUID().toString}")),
      "userId" -> Some(encrypt(user.userId)),
      FrontEndRedirect.redirectSessionKey -> redirectUrlForPaye)

    val cleanMap = session.collect {
      case (k, Some(v)) => k -> v
    }

    Session(cleanMap)
  }

  private def assertRedirected(result: SimpleResult, expectedLocation: String) {
    result.header.status shouldBe 303
    result.header.headers.get("Location") shouldBe Some(expectedLocation)
  }

  private def assertRedirectedToLoginWithNewSession(result: SimpleResult) {

    result.header.status shouldBe 303
    result.header.headers.get("Location") shouldBe Some(commonRoutes.LoginController.login().url)

    val cookies = Cookies(result.header.headers.get(SET_COOKIE))
    cookies should not be 'empty

    val sessionCookie = cookies.get("PLAY_SESSION")

    sessionCookie should not be None
    sessionCookie.get.value shouldBe ""
  }
}


