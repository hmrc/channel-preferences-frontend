package connectors

import controllers.internal.ServiceActivationController
import model.HostContext
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import play.api.test.FakeRequest
import service.{PaperlessActivateService, PreferenceFound, UnAuthorised}
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.domain.TaxIds.TaxIdWithName
import uk.gov.hmrc.play.frontend.auth
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec
import play.api.test.Helpers._

import scala.concurrent.Future

class ActivationControllerSpec extends UnitSpec with MockitoSugar with ScalaFutures with OneAppPerSuite {
  "activationController" should {

    implicit val hc = HeaderCarrier()

    "not call the new flow when a preference is found in the old flow" in {
      val serviceMock = mock[PaperlessActivateService]

      val entityResolverConnectorMock = mock[EntityResolverConnector]
      when(entityResolverConnectorMock.getPreferencesStatus()(hc)).thenReturn(Future.successful(Right(SaPreference(true, None))))

      val authConnectorMock = mock[AuthConnector]
      when(authConnectorMock.currentTaxIdentifiers).thenReturn(Future.successful(Set[TaxIdWithName](SaUtr("123456789"))))

      val controller = new ServiceActivationController {
        override val activationService: PaperlessActivateService = serviceMock

        override protected def authConnector: auth.connectors.AuthConnector = authConnectorMock

        override def entityResolverConnector: EntityResolverConnector = entityResolverConnectorMock

        override val hostUrl: String = ""
      }

      controller.paperlessStatusFor(HostContext("link", "linkText"), "default")
      verifyZeroInteractions(serviceMock)
    }

    "call the new flow when a preference is not  found in the old flow" in {
      val serviceMock = mock[PaperlessActivateService]
      when(serviceMock.paperlessPreference(any(), any())(any(), any())).thenReturn(Future.successful(PreferenceFound))

      val entityResolverConnectorMock = mock[EntityResolverConnector]
      when(entityResolverConnectorMock.getPreferencesStatus()(any())).thenReturn(Future.successful(Left(412)))

      val authConnectorMock = mock[AuthConnector]
      when(authConnectorMock.currentTaxIdentifiers).thenReturn(Future.successful(Set[TaxIdWithName](SaUtr("123456789"))))

      lazy val controller = new ServiceActivationController {
        override  val activationService: PaperlessActivateService = serviceMock

        override protected val authConnector: auth.connectors.AuthConnector = authConnectorMock

        override  val entityResolverConnector: EntityResolverConnector = entityResolverConnectorMock

        override val hostUrl: String = ""
      }

      val result = controller.checkPreferenceActivationFlows(HostContext("link", "linkText"), "default").futureValue
      verify(serviceMock, times(1)).paperlessPreference(any(), any())(any(), any())

      status(result) shouldBe OK

    }
  }
}
