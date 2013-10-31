package controllers.bt.epaye

import org.mockito.Mockito._
import uk.gov.hmrc.common.BaseSpec
import play.api.test.Helpers._
import concurrent.Future
import play.api.test.{FakeRequest, WithApplication, FakeApplication}
import controllers.bt.testframework.mocks.PortalUrlBuilderMock
import controllers.bt.EpayeController
import uk.gov.hmrc.common.microservice.domain.{RegimeRoots, User}
import uk.gov.hmrc.common.microservice.auth.domain.{Regimes, UserAuthority}
import uk.gov.hmrc.domain.EmpRef
import uk.gov.hmrc.common.microservice.epaye.domain.{EpayeLinks, EpayeRoot}

class EpayeControllerSpec extends BaseSpec {

  private def epayeRoot = Some(EpayeRoot(EmpRef("emp/ref"), EpayeLinks(None)))

  "render the Make a Payment page" in new WithApplication(FakeApplication()) with PortalUrlBuilderMock  {

    val controllerUnderTest = new EpayeController with MockedPortalUrlBuilder

    implicit val user = User(userId = "userId", userAuthority = UserAuthority("userId", Regimes()), nameFromGovernmentGateway = Some("Ciccio"), regimes = RegimeRoots(epaye = epayeRoot), decryptedToken = None)
    implicit val request = FakeRequest()

    when(mockPortalUrlBuilder.buildPortalUrl("btDirectDebits")).thenReturn("epayeDirectDebitsUrl")

    val result = Future.successful(controllerUnderTest.makeAPaymentPage(user, request))

    status(result) shouldBe 200
    verify(mockPortalUrlBuilder).buildPortalUrl("btDirectDebits")
  }


}


