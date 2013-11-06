package controllers.common

import org.scalatest.mock.MockitoSugar
import java.util.UUID
import uk.gov.hmrc.common.BaseSpec
import play.api.test.WithApplication
import play.api.test.Helpers._
import play.api.test.FakeApplication
import play.api.mvc.{Session, SimpleResult, Cookies}
import controllers.common.{routes => commonRoutes}
import uk.gov.hmrc.common.microservice.domain.{RegimeRoots, User}
import uk.gov.hmrc.common.microservice.auth.domain.{Regimes, UserAuthority}
import uk.gov.hmrc.common.microservice.paye.domain.PayeRoot
import uk.gov.hmrc.common.microservice.sa.domain.SaRoot
import uk.gov.hmrc.common.microservice.vat.domain.VatRoot
import uk.gov.hmrc.common.microservice.ct.domain.CtRoot
import uk.gov.hmrc.common.microservice.epaye.domain.EpayeRoot
import uk.gov.hmrc.common.microservice.agent.AgentRoot
import java.net.URI

class HomeControllerSpec extends BaseSpec with MockitoSugar with CookieEncryption {

  private abstract class WithSetup extends WithApplication(FakeApplication()) {
    val controller = new HomeController
  }

  "Calling redirectToLoginPage" should {

    "redirect to the login page and delete the session" in new WithSetup {
      val result = controller.redirectToLoginPage
      assertRedirectedToLoginWithNewSession(result)
    }
  }

  "Calling redirectToHomepage" should {

    "end the session and redirect to the login page if the user has no regimes and is not a Government Gateway user" in new WithSetup {
      val user = makeUser(Regimes())
      val result = controller.redirectToHomepage(user, sessionFor(user))
      assertRedirectedToLoginWithNewSession(result)
    }

    "redirect to the PAYE homepage if the user is in PAYE (only) and has no redirect in the session" in new WithSetup {
      val user = makeUser(Regimes(paye = Some(URI.create("/paye/nino/AB123456C"))))
      val result = controller.redirectToHomepage(user, sessionFor(user))
      assertRedirected(result, FrontEndRedirect.payeHome)
    }

    "redirect to the page in the session if the user is in PAYE (only) and has a redirect in the session" in new WithSetup {
      val user = makeUser(Regimes(paye = Some(URI.create("/paye/nino/AB123456C"))))
      val result = controller.redirectToHomepage(user, sessionFor(user, Some("/some/page")))
      assertRedirected(result, "/some/page")
    }

    "redirect to the PAYE homepage if the user is in PAYE and Agent and has no redirect in the session" in new WithSetup {
      val user = makeUser(Regimes(paye = Some(URI.create("/paye/nino/AB123456C")), agent = Some(URI.create("/agent/1234"))))
      val result = controller.redirectToHomepage(user, sessionFor(user, Some("/some/page")))
      assertRedirected(result, "/some/page")
    }

    "redirect to the page in the session if the user is in PAYE and Agent and has a redirect in the session" in new WithSetup {
      val user = makeUser(Regimes(paye = Some(URI.create("/paye/nino/AB123456C")), agent = Some(URI.create("/agent/1234"))))
      val result = controller.redirectToHomepage(user, sessionFor(user, Some("/some/page")))
      assertRedirected(result, "/some/page")
    }

    "redirect to the business tax homepage if the user is in SA (only)" in new WithSetup {
      val user = makeUser(Regimes(sa = Some(URI.create("/sa/individual/1234543210"))))
      val result = controller.redirectToHomepage(user, sessionFor(user))
      assertRedirected(result, FrontEndRedirect.businessTaxHome)
    }

    "redirect to the business tax homepage if the user is in VAT (only)" in new WithSetup {
      val user = makeUser(Regimes(vat = Some(URI.create("/vat/234543210"))))
      val result = controller.redirectToHomepage(user, sessionFor(user))
      assertRedirected(result, FrontEndRedirect.businessTaxHome)
    }

    "redirect to the business tax homepage if the user is in CT (only)" in new WithSetup {
      val user = makeUser(Regimes(ct = Some(URI.create("/ct/1234543210"))))
      val result = controller.redirectToHomepage(user, sessionFor(user))
      assertRedirected(result, FrontEndRedirect.businessTaxHome)
    }

    "redirect to the business tax homepage if the user is in EPAYE (only)" in new WithSetup {
      val user = makeUser(Regimes(epaye = Some(URI.create("/epaye/abc/123"))))
      val result = controller.redirectToHomepage(user, sessionFor(user))
      assertRedirected(result, FrontEndRedirect.businessTaxHome)
    }

    "redirect to the agent homepage if the user is an Agent (only)" in new WithSetup {
      val user = makeUser(Regimes(agent = Some(URI.create("/agent/12365"))))
      val result = controller.redirectToHomepage(user, sessionFor(user))
      assertRedirected(result, FrontEndRedirect.agentHome)
    }

    "redirect to the business tax homepage if the user has a combination of business tax enrolments" in new WithSetup {

      val user = makeUser(Regimes(
        epaye = Some(URI.create("/epaye/abc/123")),
        ct = Some(URI.create("/ct/1234543210")),
        vat = Some(URI.create("/vat/234543210")),
        sa = Some(URI.create("/sa/individual/1234543210"))))

      val result = controller.redirectToHomepage(user, sessionFor(user))
      assertRedirected(result, FrontEndRedirect.businessTaxHome)
    }

    "redirect to the business tax homepage if the user has no enrolments, but is a Government Gateway user" in new WithSetup {
      val user = makeUser(Regimes(), Some("Geoff Fisher"))
      val result = controller.redirectToHomepage(user, sessionFor(user))
      assertRedirected(result, FrontEndRedirect.businessTaxHome)
    }

    "redirect to the business tax homepage if the user has a combination of Agent and business tax enrolments" in new WithSetup {
      val user1 = makeUser(Regimes(
        epaye = Some(URI.create("/epaye/abc/123")),
        ct = Some(URI.create("/ct/1234543210")),
        vat = Some(URI.create("/vat/234543210")),
        sa = Some(URI.create("/sa/individual/1234543210")),
        agent = Some(URI.create("/agent/12365"))))

      val result1 = controller.redirectToHomepage(user1, sessionFor(user1))
      assertRedirected(result1, FrontEndRedirect.businessTaxHome)

      val user2 = makeUser(Regimes(
        vat = Some(URI.create("/vat/333222111")),
        agent = Some(URI.create("/agent/34543"))))

      val result2 = controller.redirectToHomepage(user2, sessionFor(user2))
      assertRedirected(result2, FrontEndRedirect.businessTaxHome)
    }
  }

  private def makeUser(regimes: Regimes, nameFromGovernmentGateway: Option[String] = None) = {

    val governmentGatewayToken = nameFromGovernmentGateway.map(_ => "<token>Geoff Fisher</token>")

    User(
      userId = "/auth/oid/someUser",
      userAuthority = mock[UserAuthority],
      regimes = RegimeRoots(
        paye = regimes.paye.map(_ => mock[PayeRoot]),
        sa = regimes.sa.map(_ => mock[SaRoot]),
        ct = regimes.ct.map(_ => mock[CtRoot]),
        vat = regimes.vat.map(_ => mock[VatRoot]),
        epaye = regimes.epaye.map(_ => mock[EpayeRoot]),
        agent = regimes.agent.map(_ => mock[AgentRoot])
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


