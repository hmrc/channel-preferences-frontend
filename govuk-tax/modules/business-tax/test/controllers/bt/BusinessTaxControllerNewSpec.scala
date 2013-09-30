package controllers.bt

import uk.gov.hmrc.common.BaseSpec
import org.scalatest.mock.MockitoSugar
import controllers.common.CookieEncryption
import play.api.test.{ WithApplication, FakeRequest }
import uk.gov.hmrc.common.microservice.auth.AuthMicroService
import uk.gov.hmrc.common.microservice.paye.PayeMicroService
import uk.gov.hmrc.common.microservice.saml.SamlMicroService
import uk.gov.hmrc.microservice.governmentgateway.GovernmentGatewayMicroService
import uk.gov.hmrc.microservice.txqueue.TxQueueMicroService
import uk.gov.hmrc.common.microservice.audit.AuditMicroService
import uk.gov.hmrc.common.microservice.keystore.KeyStoreMicroService
import uk.gov.hmrc.common.microservice.agent.AgentMicroService
import uk.gov.hmrc.common.microservice.epaye.EPayeConnector
import org.mockito.Mockito._
import controllers.common.service.MicroServices
import play.api.mvc.{Request, AnyContent, Action, Result}
import java.util.UUID
import controllers.common.SessionTimeoutWrapper._
import org.joda.time.{Duration, DateTimeZone, DateTime}
import play.api.test.Helpers._
import play.api.templates.Html
import java.net.URI
import uk.gov.hmrc.domain._
import play.api.Logger
import uk.gov.hmrc.common.microservice.epaye.domain.EPayeDomain.EPayeLinks
import uk.gov.hmrc.domain.EmpRef
import uk.gov.hmrc.common.microservice.auth.domain.UserAuthority
import uk.gov.hmrc.common.microservice.sa.domain.SaRoot
import uk.gov.hmrc.domain.Vrn
import uk.gov.hmrc.common.microservice.auth.domain.Regimes
import uk.gov.hmrc.domain.CtUtr
import uk.gov.hmrc.common.microservice.domain.User
import play.api.test.FakeApplication
import uk.gov.hmrc.common.microservice.vat.domain.VatDomain.VatRoot
import uk.gov.hmrc.common.microservice.epaye.domain.EPayeDomain.EPayeRoot
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.common.microservice.domain.RegimeRoots
import uk.gov.hmrc.common.microservice.paye.domain.PayeRoot
import controllers.bt.regimeViews.{AccountSummariesFactory, AccountSummaries}
import uk.gov.hmrc.common.microservice.sa.SaConnector
import uk.gov.hmrc.common.microservice.vat.VatConnector
import uk.gov.hmrc.common.microservice.ct.CtConnector
import uk.gov.hmrc.common.microservice.ct.domain.CtDomain.CtRoot
import org.mockito.Matchers

trait BusinessTaxControllerBehaviours extends BaseSpec {

  def aBusinessUserSessionValidatingMethod(method: BusinessTaxController => Action[AnyContent]) = {

    "redirect if there is no session" in new WithBusinessTaxApplication {
      val result: Result = method(businessTaxController)(FakeRequest())
      status(result) shouldBe 303
    }

    "redirect if there is no logged in user" in new WithBusinessTaxApplication {
      val result: Result = businessTaxController.makeAPaymentLanding(request)
      status(result) shouldBe 303
    }

    "redirect if the session has timed out" in new WithBusinessTaxApplication with GeoffFisherExpectations {
      override val lastRequestTimestamp = currentTime.minus(Duration.standardMinutes(20))
      val result: Result = businessTaxController.makeAPaymentLanding(request)
      status(result) shouldBe 303
    }

    "redirect if the user is not a business tax user" in new WithBusinessTaxApplication with NonBusinessUserExpectations {
      val result: Result = businessTaxController.makeAPaymentLanding(request)
      status(result) shouldBe 303
      header("Location", result) shouldBe Some("/")
    }
  }
}

class BusinessTaxControllerStandardBehaviourSpec extends BusinessTaxControllerBehaviours {

  "Calling makeAPaymentLanding" should {
    behave like aBusinessUserSessionValidatingMethod(businessTaxController => businessTaxController.makeAPaymentLanding)
  }

  "Calling home" should {
    behave like aBusinessUserSessionValidatingMethod(businessTaxController => businessTaxController.home)
  }
}

class BusinessTaxControllerNewSpec extends BaseSpec with CookieEncryption {

  "Calling makeAPaymentLanding with a valid logged in business user" should {

    "render the Make a Payment landing page" in new WithBusinessTaxApplication with GeoffFisherExpectations {

      val expectedHtml = "<html>some html for landing page</html>"

      when (expectations.makeAPaymentLandingPage(geoffFisherUser)).thenReturn(expectedHtml)

      val result: Result = businessTaxController.makeAPaymentLanding(request)

      status(result) shouldBe 200
      contentAsString(result) shouldBe expectedHtml
    }
  }

  "Calling home with a valid logged in business user" should {

    "pass the correct data to the home page for a user in all regimes" in new WithBusinessTaxApplication with GeoffFisherExpectations {

      val geoffFisherSummaries = mock[AccountSummaries]
      val expectedHtml = "<html>some html for the Business Tax Homepage</html>"

      when (expectations.buildPortalUrl(geoffFisherUser, request, "home")).thenReturn("homeURL")

      when (expectations.businessTaxHomepage(geoffFisherUser, "homeURL", geoffFisherSummaries)).thenReturn(expectedHtml)

      when(mockAccountSummariesFactory.create(anyOfType[String => String])(Matchers.eq(geoffFisherUser))).thenReturn(geoffFisherSummaries)

      val result: Result = businessTaxController.home(request)

      status(result) shouldBe 200
      contentAsString(result) shouldBe expectedHtml
    }
  }
}

private [bt] trait MicroServiceMocks extends MockitoSugar {
  val mockAuthMicroService = mock[AuthMicroService]
  val mockPayeMicroService = mock[PayeMicroService]
  val mockSamlMicroService = mock[SamlMicroService]
  val mockSaConnector = mock[SaConnector]
  val mockGovernmentGatewayMicroService = mock[GovernmentGatewayMicroService]
  val mockTxQueueMicroService = mock[TxQueueMicroService]
  val mockAuditMicroService = mock[AuditMicroService]
  val mockKeyStoreMicroService = mock[KeyStoreMicroService]
  val mockAgentMicroService = mock[AgentMicroService]
  val mockVatConnector = mock[VatConnector]
  val mockCtConnector = mock[CtConnector]
  val mockEPayeConnector = mock[EPayeConnector]

  trait MockedMicroServices extends MicroServices {
    override lazy val authMicroService = mockAuthMicroService
    override lazy val payeMicroService = mockPayeMicroService
    override lazy val samlMicroService = mockSamlMicroService
    override lazy val saConnector = mockSaConnector
    override lazy val governmentGatewayMicroService = mockGovernmentGatewayMicroService
    override lazy val txQueueMicroService = mockTxQueueMicroService
    override lazy val auditMicroService = mockAuditMicroService
    override lazy val keyStoreMicroService = mockKeyStoreMicroService
    override lazy val agentMicroService = mockAgentMicroService
    override lazy val vatConnector = mockVatConnector
    override lazy val ctConnector = mockCtConnector
    override lazy val epayeConnector = mockEPayeConnector
  }
}


abstract class WithBusinessTaxApplication extends WithApplication(FakeApplication()) with MicroServiceMocks with MockitoSugar with CookieEncryption {

  val currentTime = new DateTime(2013, 9, 27, 15, 7, 22, 232, DateTimeZone.UTC)

  val lastRequestTimestamp: DateTime = currentTime.minus(Duration.standardMinutes(1))

  val mockAccountSummariesFactory = mock[AccountSummariesFactory]

  val userId: Option[String] = None

  val affinityGroup: Option[String] = None

  val nameFromGovernmentGateway: Option[String] = None

  val governmentGatewayToken: Option[String] = None

  trait Expectations {

    def makeAPaymentLandingPage(user: User): String

    def businessTaxHomepage(user: User, portalHref: String, accountSummaries: AccountSummaries): String

    def buildPortalUrl(user: User, request: Request[AnyRef], base: String): String
  }

  val expectations = mock[Expectations]

  val businessTaxController = new BusinessTaxController(mockAccountSummariesFactory) with MockedMicroServices {

    override def now: () => DateTime = () => currentTime

    override private[bt] def makeAPaymentLandingPage()(implicit user: User): Html = {
      Logger.debug("RENDERING makeAPaymentLandingPage")
      Html(expectations.makeAPaymentLandingPage(user))
    }

    override private[bt] def businessTaxHomepage(portalHref: String, accountSummaries: AccountSummaries)(implicit user: User): Html = {
      Logger.debug("RENDERING businessTaxHomePage")
      Html(expectations.businessTaxHomepage(user, portalHref, accountSummaries))
    }

    override def buildPortalUrl(base: String)(implicit request: Request[AnyRef], user: User): String = {
      expectations.buildPortalUrl(user, request, base)
    }
  }

  implicit lazy val request = {

    val session: Seq[(String, Option[String])] = Seq(
      "sessionId" -> Some(encrypt(s"session-${UUID.randomUUID().toString}")),
      lastRequestTimestampKey -> Some(lastRequestTimestamp.getMillis.toString),
      "userId" -> userId.map(encrypt),
      "name" -> nameFromGovernmentGateway.map(encrypt),
      "token" -> governmentGatewayToken.map(encrypt),
      "affinityGroup" -> affinityGroup.map(encrypt))

    val cleanSession = session.collect { case (paramName, Some(paramValue)) => (paramName, paramValue)}

    FakeRequest().withSession(cleanSession:_*)
  }
}

trait GeoffFisherExpectations {

  self: WithBusinessTaxApplication =>

  val geoffFisherAuthId = "/auth/oid/geoff"
  val geoffFisherSaUtr = SaUtr("/sa/individual/123456789012")
  val geoffFisherCtUtr = CtUtr("/ct/6666644444")
  val geoffFisherEmpRef = EmpRef("342", "sdfdsf")
  val geoffFisherVrn = Vrn("456345576")

  override val userId: Option[String] = Some(geoffFisherAuthId)

  override val affinityGroup: Option[String] = Some("someAffinityGroup")

  override val nameFromGovernmentGateway: Option[String] = Some("Geoffrey From Government Gateway")

  override val governmentGatewayToken: Option[String] = Some("someToken")

  val geoffFisherAuthority = UserAuthority(
    id = geoffFisherAuthId,
    regimes = Regimes(
      sa = Some(URI.create(geoffFisherSaUtr.toString)),
      vat = Some(URI.create(geoffFisherVrn.toString)),
      epaye = Some(URI.create(geoffFisherEmpRef.toString)),
      ct = Some(URI.create(geoffFisherCtUtr.toString))),
    previouslyLoggedInAt = Some(new DateTime(1000L)),
    saUtr = Some(geoffFisherSaUtr),
    vrn = Some(geoffFisherVrn),
    ctUtr = Some(geoffFisherCtUtr),
    empRef = Some(geoffFisherEmpRef))

  val geoffFisherSaRoot = SaRoot(utr = geoffFisherSaUtr.toString, links = Map("something" -> s"$geoffFisherSaUtr/stuff"))

  val geoffFisherVatRoot = VatRoot(vrn = geoffFisherVrn, links = Map("something" -> s"$geoffFisherVrn/stuff"))

  val geoffFisherEPayeRoot = EPayeRoot(links = EPayeLinks(accountSummary = Some(s"$geoffFisherEmpRef/blah")))

  val geoffFisherCtRoot = CtRoot(links = Map("something" -> s"$geoffFisherCtUtr/dsffds"))

  implicit val geoffFisherUser = User(
    user = geoffFisherAuthId,
    userAuthority = geoffFisherAuthority,
    regimes = RegimeRoots(paye = None, sa = Some(geoffFisherSaRoot), vat = Some(geoffFisherVatRoot), epaye = Some(geoffFisherEPayeRoot), ct = Some(geoffFisherCtRoot)),
    nameFromGovernmentGateway = nameFromGovernmentGateway,
    decryptedToken = governmentGatewayToken
  )

  when(mockAuthMicroService.authority(geoffFisherAuthId)).thenReturn(Some(geoffFisherAuthority))
  when(mockSaConnector.root(geoffFisherSaUtr.toString)).thenReturn(geoffFisherSaRoot)
  when(mockVatConnector.root(geoffFisherVrn.toString)).thenReturn(geoffFisherVatRoot)
  when(mockEPayeConnector.root(geoffFisherEmpRef.toString)).thenReturn(geoffFisherEPayeRoot)
  when(mockCtConnector.root(geoffFisherCtUtr.toString)).thenReturn(geoffFisherCtRoot)
}


trait NonBusinessUserExpectations {

  self: WithBusinessTaxApplication =>

  val johnDensmoreAuthId = "/auth/oid/densmore"
  val johnDensmoreNino = Nino("AB654433C")

  override val userId: Option[String] = Some(johnDensmoreAuthId)

  val johnDensmoreAuthority = UserAuthority(
    id = johnDensmoreAuthId,
    regimes = Regimes(paye = Some(URI.create(s"/paye/$johnDensmoreNino"))),
    nino = Some(johnDensmoreNino))

  val johnDensmorePayeRoot = mock[PayeRoot]

  val johnDensmoreUser = User(
    user = johnDensmoreAuthId,
    userAuthority = johnDensmoreAuthority,
    regimes = RegimeRoots(paye = Some(johnDensmorePayeRoot), sa = None, vat = None, epaye = None, ct = None),
    nameFromGovernmentGateway = None,
    decryptedToken = None
  )

  when(mockAuthMicroService.authority(johnDensmoreAuthId)).thenReturn(Some(johnDensmoreAuthority))
  when(mockPayeMicroService.root(johnDensmoreNino.toString)).thenReturn(johnDensmorePayeRoot)
}

