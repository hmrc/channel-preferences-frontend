package controllers.bt.testframework.mocks

import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.common.PortalUrlBuilder
import play.api.mvc.Request
import uk.gov.hmrc.common.microservice.domain.User

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
