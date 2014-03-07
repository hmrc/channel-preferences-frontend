package controllers.bt.testframework.request

import java.net.URI
import org.mockito.Mockito._
import controllers.bt.testframework.fixtures.BusinessUserFixture
import java.util.UUID
import controllers.common.SessionTimeoutWrapper._
import play.api.test.FakeRequest
import uk.gov.hmrc.common.microservice.epaye.EpayeConnector
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import uk.gov.hmrc.common.microservice.sa.SaConnector
import uk.gov.hmrc.common.microservice.vat.VatConnector
import uk.gov.hmrc.common.microservice.ct.CtConnector
import controllers.common.actions.HeaderCarrier
import scala.concurrent.Future
import uk.gov.hmrc.common.microservice.sa.domain.SaJsonRoot
import scala.Some
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.common.microservice.domain.RegimeRoots
import uk.gov.hmrc.common.microservice.ct.domain.CtJsonRoot
import uk.gov.hmrc.common.microservice.epaye.domain.EpayeJsonRoot
import uk.gov.hmrc.common.microservice.vat.domain.VatJsonRoot
import controllers.common.SessionKeys

trait BusinessTaxRequest extends BusinessUserFixture with MockitoSugar {

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

  when(mockAuthConnector.currentAuthority).thenReturn(Future.successful(Some(authority)))

  saRootLink.map(link => when(mockSaConnector.root(link.toString)).thenReturn(Future.successful(saJsonRoot.get)))
  vatRootLink.map(link => when(mockVatConnector.root(link.toString)).thenReturn(Future.successful(vatJsonRoot.get)))
  epayeRootLink.map(link => when(mockEpayeConnector.root(link.toString)).thenReturn(Future.successful(epayeJsonRoot.get)))
  ctRootLink.map(link => when(mockCtConnector.root(link.toString)).thenReturn(Future.successful(ctJsonRoot.get)))

  implicit lazy val user = User(
    userId = authority.uri,
    userAuthority = authority,
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
      SessionKeys.sessionId -> Some(s"session-${UUID.randomUUID().toString}"),
      SessionKeys.lastRequestTimestamp -> lastRequestTimestamp.map(_.getMillis.toString),
      SessionKeys.userId -> Some(userId),
      SessionKeys.name -> nameFromGovernmentGateway,
      SessionKeys.token -> governmentGatewayToken)

    val cleanSession = session.collect {
      case (paramName, Some(paramValue)) => (paramName, paramValue)
    }

    FakeRequest().withSession(cleanSession: _*)
  }
}
