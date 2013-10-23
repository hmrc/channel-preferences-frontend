package controllers.bt.ct

import play.api.test.{FakeApplication, WithApplication}
import controllers.common.CookieEncryption
import controllers.bt.CtController
import controllers.bt.testframework.mocks.{PortalUrlBuilderMock, DateTimeProviderMock, ConnectorMocks}
import play.api.templates.Html
import uk.gov.hmrc.common.microservice.domain.User
import org.scalatest.mock.MockitoSugar

abstract class CtControllerForTest extends WithApplication(FakeApplication()) with CookieEncryption with PortalUrlBuilderMock with ConnectorMocks with DateTimeProviderMock with CtPageMocks {

  val controllerUnderTest = new CtController with MockedPortalUrlBuilder with MockedConnectors with MockedDateTimeProvider with MockedCtPages
}


trait CtPageMocks extends MockitoSugar {

  val mockCtPages = mock[MockableCtPages]

  trait MockableCtPages {
    def makeAPaymentPage(ctOnlineAccount: String, ctDirectDebits: String): Html
  }

  trait MockedCtPages {
    self: CtController =>

    private[bt] override def makeAPaymentPage(ctOnlineAccount: String, ctDirectDebits: String)(implicit user: User): Html = {
      mockCtPages.makeAPaymentPage(ctOnlineAccount, ctDirectDebits)
    }
  }
}