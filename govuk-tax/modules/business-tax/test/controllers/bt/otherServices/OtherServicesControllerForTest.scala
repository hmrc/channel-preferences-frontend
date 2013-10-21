package controllers.bt.otherservices

import play.api.test.{FakeApplication, WithApplication}
import controllers.common.CookieEncryption
import controllers.bt.testframework.mocks.{PortalUrlBuilderMock, DateTimeProviderMock, ConnectorMocks}
import org.scalatest.mock.MockitoSugar
import play.api.templates.Html
import uk.gov.hmrc.common.microservice.domain.User
import controllers.bt.OtherServicesController

abstract class OtherServicesControllerForTest extends WithApplication(FakeApplication()) with CookieEncryption with PortalUrlBuilderMock with ConnectorMocks with DateTimeProviderMock with OtherServicesPageMocks {

  val mockOtherServicesFactory = mock[OtherServicesFactory]

  val controllerUnderTest = new OtherServicesController(mockOtherServicesFactory) with MockedPortalUrlBuilder with MockedConnectors with MockedDateTimeProvider with MockedOtherServicesPages
}


trait OtherServicesPageMocks extends MockitoSugar {

  val mockOtherServicesPages = mock[MockableOtherServicesPages]

  trait MockableOtherServicesPages {
    def otherServicesPage(otherServicesSummary: OtherServicesSummary): Html
  }

  trait MockedOtherServicesPages {
    self: OtherServicesController =>

    override private[bt] def otherServicesPage(otherServicesSummary: OtherServicesSummary)(implicit user: User): Html = {
      mockOtherServicesPages.otherServicesPage(otherServicesSummary)
    }
  }
}