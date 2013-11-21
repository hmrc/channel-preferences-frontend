package controllers.bt.testframework.request

import java.net.URI
import org.mockito.Mockito._
import controllers.bt.testframework.fixtures.BusinessUserFixture
import controllers.common.CookieEncryption
import java.util.UUID
import controllers.common.SessionTimeoutWrapper._
import uk.gov.hmrc.common.microservice.auth.domain.UserAuthority
import uk.gov.hmrc.common.microservice.auth.domain.Regimes
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.common.microservice.domain.RegimeRoots
import play.api.test.FakeRequest
import uk.gov.hmrc.common.microservice.sa.domain.SaJsonRoot
import uk.gov.hmrc.common.microservice.vat.domain.VatJsonRoot
import uk.gov.hmrc.common.microservice.ct.domain.CtJsonRoot
import uk.gov.hmrc.common.microservice.epaye.domain.EpayeJsonRoot
import uk.gov.hmrc.common.microservice.epaye.EpayeConnector
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import uk.gov.hmrc.common.microservice.sa.SaConnector
import uk.gov.hmrc.common.microservice.vat.VatConnector
import uk.gov.hmrc.common.microservice.ct.CtConnector
import controllers.common.actions.HeaderCarrier

trait BusinessTaxRequest extends CookieEncryption with BusinessUserFixture with MockitoSugar {

  private def saUtrOpt = saRoot.map(_.utr)

  private def empRefOpt = epayeRoot.map(_.empRef)

  private def ctUtrOpt = ctRoot.map(_.utr)

  private def vrnOpt = vatRoot.map(_.vrn)

  private def saRootLink = saRoot.map(root => URI.create("/sa/" + root.utr.toString))

  private def ctRootLink = ctRoot.map(root => URI.create("/ct/" + root.utr.toString))

  private def vatRootLink = vatRoot.map(root => URI.create("/vat/" + root.vrn.toString))

  private def epayeRootLink = epayeRoot.map(root => URI.create("/epaye/" + root.empRef.toString))

  private def saJsonRoot = saRoot.map(root => SaJsonRoot(root.links))

  private def vatJsonRoot = vatRoot.map(root => VatJsonRoot(root.links))

  private def ctJsonRoot = ctRoot.map(root => CtJsonRoot(root.links))

  private def epayeJsonRoot = epayeRoot.map(root => EpayeJsonRoot(root.links))

  val mockAuthConnector = mock[AuthConnector]
  val mockSaConnector = mock[SaConnector]
  val mockVatConnector = mock[VatConnector]
  val mockCtConnector = mock[CtConnector]
  val mockEpayeConnector = mock[EpayeConnector]


  implicit val hc = HeaderCarrier()


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

  when(mockAuthConnector.authority(userId)).thenReturn(Some(userAuthority))

  saRootLink.map(link => when(mockSaConnector.root(link.toString)).thenReturn(saJsonRoot.get))
  vatRootLink.map(link => when(mockVatConnector.root(link.toString)).thenReturn(vatJsonRoot.get))
  epayeRootLink.map(link => when(mockEpayeConnector.root(link.toString)).thenReturn(epayeJsonRoot.get))
  ctRootLink.map(link => when(mockCtConnector.root(link.toString)).thenReturn(ctJsonRoot.get))

  implicit lazy val user = User(
    userId = userAuthority.id,
    userAuthority = userAuthority,
    regimes = RegimeRoots(
      paye = None,
      sa = saRoot,
      vat = vatRoot,
      epaye = epayeRoot,
      ct = ctRoot),
    nameFromGovernmentGateway = nameFromGovernmentGateway,
    decryptedToken = governmentGatewayToken
  )

  def request = {

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
