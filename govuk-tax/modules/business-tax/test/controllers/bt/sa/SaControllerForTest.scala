package controllers.bt.sa

import play.api.test.{FakeApplication, WithApplication}
import controllers.common.CookieEncryption
import controllers.bt.SaController
import controllers.bt.testframework.mocks.{PortalUrlBuilderMock, DateTimeProviderMock, ConnectorMocks}
import play.api.templates.Html
import uk.gov.hmrc.common.microservice.domain.User
import org.scalatest.mock.MockitoSugar
import views.helpers.RenderableMessage

abstract class SaControllerForTest extends WithApplication(FakeApplication()) with CookieEncryption with PortalUrlBuilderMock with ConnectorMocks with DateTimeProviderMock with SaPageMocks {

  val controllerUnderTest = new SaController with MockedPortalUrlBuilder with MockedConnectors with MockedDateTimeProvider with MockedSaPages
}


trait SaPageMocks extends MockitoSugar {

  val mockSaPages = mock[MockableSaPages]

  trait MockableSaPages {
    def makeAPaymentPage(saDirectDebitsLink: RenderableMessage, utr: String): Html
  }

  trait MockedSaPages {
    self: SaController =>

    private[bt] override def makeAPaymentPage(saDirectDebitsLink: RenderableMessage, utr: String)(implicit user: User): Html = {
      mockSaPages.makeAPaymentPage(saDirectDebitsLink, utr)
    }
  }
}