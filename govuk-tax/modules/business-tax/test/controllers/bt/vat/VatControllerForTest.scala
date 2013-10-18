package controllers.bt.vat

import play.api.test.{FakeApplication, WithApplication}
import controllers.common.CookieEncryption
import controllers.bt.VatController
import controllers.bt.mixins.mocks.{PortalUrlBuilderMock, DateTimeProviderMock, ConnectorMocks}

abstract class VatControllerForTest extends WithApplication(FakeApplication()) with CookieEncryption with PortalUrlBuilderMock with ConnectorMocks with DateTimeProviderMock with VatPageMocks {

  val vatControllerUnderTest = new VatController with MockedPortalUrlBuilder with MockedConnectors with MockedDateTimeProvider with MockedVatPages
}
