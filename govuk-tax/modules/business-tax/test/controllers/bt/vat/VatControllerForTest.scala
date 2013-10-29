package controllers.bt.vat

import play.api.test.{FakeApplication, WithApplication}
import controllers.common.CookieEncryption
import controllers.bt.VatController
import controllers.bt.testframework.mocks.{PortalUrlBuilderMock, DateTimeProviderMock, ConnectorMocks}
import org.scalatest.mock.MockitoSugar
import play.api.templates.Html
import uk.gov.hmrc.common.microservice.domain.User
import views.helpers.RenderableMessage

abstract class VatControllerForTest extends WithApplication(FakeApplication()) with CookieEncryption with PortalUrlBuilderMock with ConnectorMocks with DateTimeProviderMock with VatPageMocks {

  val controllerUnderTest = new VatController with MockedPortalUrlBuilder with MockedConnectors with MockedDateTimeProvider with MockedVatPages
}


trait VatPageMocks extends MockitoSugar {

  val mockVatPages = mock[MockableVatPages]

  trait MockableVatPages {
    def makeAPaymentPage(vatOnlineAccountLink: RenderableMessage): Html
  }

  trait MockedVatPages {
    self: VatController =>

    private[bt] override def makeAPaymentPage(vatOnlineAccountLink: RenderableMessage)(implicit user: User): Html = {
      mockVatPages.makeAPaymentPage(vatOnlineAccountLink)
    }
  }

}