package controllers.bt.vat


import uk.gov.hmrc.common.{PortalUrlBuilder, BaseSpec}
import controllers.common.CookieEncryption
import org.mockito.Mockito._
import play.api.mvc.{Request, Result}
import play.api.test.Helpers._
import org.joda.time.{Duration, DateTimeZone, DateTime}
import uk.gov.hmrc.common.microservice.domain.RegimeRoots
import play.api.templates.Html
import org.scalatest.mock.MockitoSugar
import controllers.common.service.MicroServices
import uk.gov.hmrc.common.microservice.auth.AuthMicroService
import uk.gov.hmrc.common.microservice.paye.PayeMicroService
import uk.gov.hmrc.common.microservice.saml.SamlMicroService
import uk.gov.hmrc.common.microservice.sa.SaConnector
import uk.gov.hmrc.microservice.governmentgateway.GovernmentGatewayMicroService
import uk.gov.hmrc.microservice.txqueue.TxQueueMicroService
import uk.gov.hmrc.common.microservice.audit.AuditMicroService
import uk.gov.hmrc.common.microservice.keystore.KeyStoreMicroService
import uk.gov.hmrc.common.microservice.agent.AgentMicroService
import uk.gov.hmrc.common.microservice.vat.VatConnector
import uk.gov.hmrc.common.microservice.ct.CtConnector
import uk.gov.hmrc.common.microservice.epaye.EpayeConnector
import controllers.bt.VatController
import play.api.test.{FakeRequest, WithApplication}
import java.util.UUID
import controllers.common.SessionTimeoutWrapper._
import uk.gov.hmrc.domain._
import java.net.URI
import uk.gov.hmrc.common.microservice.sa.domain.SaDomain.SaRoot
import uk.gov.hmrc.common.microservice.vat.domain.VatDomain.VatRoot
import uk.gov.hmrc.common.microservice.epaye.domain.EpayeDomain.EpayeRoot
import uk.gov.hmrc.common.microservice.ct.domain.CtDomain.CtRoot
import scala.util.Success
import config.DateTimeProvider
import uk.gov.hmrc.common.microservice.auth.domain.UserAuthority
import uk.gov.hmrc.common.microservice.epaye.domain.EpayeDomain.EpayeLinks
import uk.gov.hmrc.common.microservice.sa.domain.SaDomain.SaJsonRoot
import scala.Some
import uk.gov.hmrc.domain.Vrn
import uk.gov.hmrc.common.microservice.auth.domain.Regimes
import uk.gov.hmrc.domain.CtUtr
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.common.microservice.epaye.domain.EpayeDomain.EpayeJsonRoot
import play.api.test.FakeApplication
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.common.microservice.ct.domain.CtDomain.CtJsonRoot
import uk.gov.hmrc.common.microservice.vat.domain.VatDomain.VatJsonRoot

class VatControllerSpec extends BaseSpec {

  "Calling makeAPayment with a valid logged in business user" should {

    "render the Make a Payment landing page" in new VatControllerForTest with GeoffFisherTestFixture with BusinessTaxRequest {
      val expectedHtml = "<html>happy Canadian thanksgiving</html>"
      when(mockPortalUrlBuilder.buildPortalUrl("vatOnlineAccount")).thenReturn("vatOnlineAccountUrl")
      when(mockVatPages.makeAPaymentPage("vatOnlineAccountUrl")).thenReturn(Html(expectedHtml))
      val result: Result = vatControllerUnderTest.makeAPayment(request)
      status(result) shouldBe 200
      contentAsString(result) shouldBe expectedHtml
    }
  }
}

abstract class VatControllerForTest extends WithApplication(FakeApplication()) with CookieEncryption with PortalUrlBuilderMock with MicroServiceMocks with DateTimeProviderMock with VatPageMocks {

  val vatControllerUnderTest = new VatController with MockedPortalUrlBuilder with MockedMicroServices with MockedDateTimeProvider with MockedVatPages
}

trait VatPageMocks extends MockitoSugar {

  val mockVatPages = mock[MockableVatPages]

  trait MockableVatPages {
    def makeAPaymentPage(vatOnlineAccount: String): Html
  }

  trait MockedVatPages {
    self: VatController =>

    private[bt] override def makeAPaymentPage(vatOnlineAccount: String)(implicit user: User): Html = {
      mockVatPages.makeAPaymentPage(vatOnlineAccount)
    }
  }

}

trait DateTimeProviderMock {

  def currentTime: DateTime

  trait MockedDateTimeProvider {

    self: DateTimeProvider =>

    override lazy val now: () => DateTime = () => currentTime
  }

}

trait PortalUrlBuilderMock extends MockitoSugar {

  val mockPortalUrlBuilder = mock[MockablePortalUrlBuilder]

  trait MockablePortalUrlBuilder {
    def buildPortalUrl(destinationPathKey: String): String
  }

  trait MockedPortalUrlBuilder {
    self: PortalUrlBuilder =>
    override def buildPortalUrl(destinationPathKey: String)(implicit request: Request[AnyRef], user: User): String = mockPortalUrlBuilder.buildPortalUrl(destinationPathKey)
  }

}

trait MicroServiceMocks extends MockitoSugar {

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
  val mockEpayeConnector = mock[EpayeConnector]

  trait MockedMicroServices {

    self: MicroServices =>

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
    override lazy val epayeConnector = mockEpayeConnector
  }

}

trait RequestWithBusinessUserSession extends CookieEncryption {

  self: BusinessUserFixture =>

  implicit lazy val request = {

    val session: Seq[(String, Option[String])] = Seq(
      "sessionId" -> Some(encrypt(s"session-${UUID.randomUUID().toString}")),
      lastRequestTimestampKey -> lastRequestTimestamp.map(_.getMillis.toString),
      "userId" -> Some(encrypt(userId)),
      "name" -> nameFromGovernmentGateway.map(encrypt),
      "token" -> governmentGatewayToken.map(encrypt),
      "affinityGroup" -> affinityGroup.map(encrypt))

    val cleanSession = session.collect {
      case (paramName, Some(paramValue)) => (paramName, paramValue)
    }

    FakeRequest().withSession(cleanSession: _*)
  }
}

trait RequestWithNonBusinessUserSession extends CookieEncryption {

  self: NonBusinessUserFixture =>

  implicit lazy val request = {

    val session: Seq[(String, Option[String])] = Seq(
      "sessionId" -> Some(encrypt(s"session-${UUID.randomUUID().toString}")),
      lastRequestTimestampKey -> lastRequestTimestamp.map(_.getMillis.toString),
      "userId" -> Some(encrypt(userId)))

    val cleanSession = session.collect {
      case (paramName, Some(paramValue)) => (paramName, paramValue)
    }

    FakeRequest().withSession(cleanSession: _*)
  }
}

trait RequestWithEmptySession extends CookieEncryption {

  self: DateTimeProviderMock =>

  implicit lazy val request = {
    val session = ("sessionId", encrypt(s"session-${UUID.randomUUID().toString}"))
    FakeRequest().withSession(session)
  }

  override def currentTime: DateTime = new DateTime(2013, 2, 11, 11, 55, 22, 555, DateTimeZone.UTC)
}

trait RequestWithoutSession {

  self: DateTimeProviderMock =>

  implicit lazy val request = FakeRequest()
  override def currentTime: DateTime = new DateTime(2013, 2, 11, 11, 55, 22, 555, DateTimeZone.UTC)
}

trait BusinessTaxRequest extends RequestWithBusinessUserSession {

  self: MicroServiceMocks with DateTimeProviderMock with BusinessUserFixture =>

  private def saUtrOpt: Option[SaUtr] = saRoot.map(_.utr)
  private def empRefOpt = epayeRoot.map(_.empRef)
  private def ctUtrOpt = ctRoot.map(_.utr)
  private def vrnOpt = vatRoot.map(_.vrn)

  private def saRootLink = saRoot.map(root => URI.create("/sa/" + root.utr.toString))
  private def ctRootLink = ctRoot.map(root => URI.create("/ct/" + root.utr.toString))
  private def vatRootLink = vatRoot.map(root => URI.create("/vat/" + root.vrn.toString))
  private def epayeRootLink = epayeRoot.map(root => URI.create("/epaye/" + root.empRef.toString))

  private def saJsonRoot = saRoot.map(root => SaJsonRoot(root.links))
  private def vatJsonRoot = ctRoot.map(root => VatJsonRoot(root.links))
  private def ctJsonRoot = vatRoot.map(root => CtJsonRoot(root.links))
  private def epayeJsonRoot = epayeRoot.map(root => EpayeJsonRoot(root.links))

  private def saRootSuccess = saRoot.map(Success(_))
  private def ctRootSuccess = ctRoot.map(Success(_))
  private def vatRootSuccess = vatRoot.map(Success(_))
  private def epayeRootSuccess = epayeRoot.map(Success(_))

  private def userAuthority = UserAuthority(
    id = userId,
    regimes = Regimes(
      sa = saRootLink,
      vat = vatRootLink,
      epaye = epayeRootLink,
      ct = ctRootLink),
    previouslyLoggedInAt = lastLoginTimestamp,
    saUtr = saUtrOpt,
    vrn = vrnOpt,
    ctUtr = ctUtrOpt,
    empRef = empRefOpt)

  when(mockAuthMicroService.authority(userId)).thenReturn(Some(userAuthority))

  if (saJsonRoot.isDefined) when(mockSaConnector.root(saUtrOpt.toString)).thenReturn(saJsonRoot.get)
  if (vatJsonRoot.isDefined) when(mockVatConnector.root(vrnOpt.toString)).thenReturn(vatJsonRoot.get)
  if (epayeJsonRoot.isDefined) when(mockEpayeConnector.root(empRefOpt.toString)).thenReturn(epayeJsonRoot.get)
  if (ctJsonRoot.isDefined) when(mockCtConnector.root(ctUtrOpt.toString)).thenReturn(ctJsonRoot.get)

  implicit lazy val user = User(
    userId = userAuthority.id,
    userAuthority = userAuthority,
    regimes = RegimeRoots(
      paye = None,
      sa = saRootSuccess,
      vat = vatRootSuccess,
      epaye = epayeRootSuccess,
      ct = ctRootSuccess),
    nameFromGovernmentGateway = nameFromGovernmentGateway,
    decryptedToken = governmentGatewayToken
  )
}


trait NonBusinessTaxRequest extends RequestWithNonBusinessUserSession {

  self: MicroServiceMocks with DateTimeProviderMock with NonBusinessUserFixture =>

  private val userAuthority = UserAuthority(
    id = userId,
    regimes = Regimes(),
    previouslyLoggedInAt = lastLoginTimestamp)

  implicit val user = User(
    userId = userAuthority.id,
    userAuthority = userAuthority,
    regimes = RegimeRoots())

  when(mockAuthMicroService.authority(userId)).thenReturn(Some(userAuthority))
}













trait UserFixture {

  def userId: String
  def currentTime: DateTime
  def lastRequestTimestamp: Option[DateTime]
  def lastLoginTimestamp: Option[DateTime]
}

trait BusinessUserFixture extends UserFixture {

  def saRoot: Option[SaRoot]
  def ctRoot: Option[CtRoot]
  def epayeRoot: Option[EpayeRoot]
  def vatRoot: Option[VatRoot]

  def affinityGroup: Option[String]
  def nameFromGovernmentGateway: Option[String]
  def governmentGatewayToken: Option[String]
}

trait NonBusinessUserFixture extends UserFixture

trait GeoffFisherTestFixture extends BusinessUserFixture {

  override val currentTime: DateTime = new DateTime(2013, 9, 27, 15, 7, 22, 232, DateTimeZone.UTC)

  val saUtr = SaUtr("/sa/individual/123456789012")
  val ctUtr = CtUtr("/ct/6666644444")
  val empRef = EmpRef("342", "sdfdsf")
  val vrn = Vrn("456345576")

  val saLinks = Map("something" -> s"$saUtr/stuff")
  val ctLinks = Map("something" -> s"$ctUtr/dsffds")
  val epayeLinks = EpayeLinks(accountSummary = Some(s"$empRef/blah"))
  val vatLinks = Map("something" -> s"$vrn/stuff")

  override val userId = "/auth/oid/geoff"

  override val saRoot = Some(SaRoot(saUtr, saLinks))
  override val ctRoot = Some(CtRoot(ctUtr, ctLinks))
  override val epayeRoot = Some(EpayeRoot(empRef, epayeLinks))
  override val vatRoot = Some(VatRoot(vrn, vatLinks))

  override val lastLoginTimestamp = Some(currentTime.minus(Duration.standardDays(14)))
  override val lastRequestTimestamp = Some(currentTime.minus(Duration.standardMinutes(1)))
  override val affinityGroup = Some("someAffinityGroup")
  override val nameFromGovernmentGateway: Option[String] = Some("Geoffrey From Government Gateway")
  override val governmentGatewayToken: Option[String] = Some("someToken")

}

trait JohnDensmoreTestFixture extends NonBusinessUserFixture {

  override val userId = "/auth/oid/densmore"

  override val currentTime: DateTime = new DateTime(2013, 9, 27, 15, 7, 22, 232, DateTimeZone.UTC)
  override val lastRequestTimestamp = Some(currentTime.minus(Duration.standardMinutes(1)))
  override val lastLoginTimestamp = Some(currentTime.minus(Duration.standardDays(14)))
}
