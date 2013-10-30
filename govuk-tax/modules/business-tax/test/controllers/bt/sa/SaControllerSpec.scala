package controllers.bt.sa

import org.mockito.Mockito._
import uk.gov.hmrc.common.BaseSpec
import play.api.test.Helpers._
import concurrent.Future
import play.api.test.{FakeRequest, WithApplication, FakeApplication}
import controllers.bt.testframework.mocks.PortalUrlBuilderMock
import controllers.bt.SaController
import uk.gov.hmrc.common.microservice.domain.{RegimeRoots, User}
import uk.gov.hmrc.common.microservice.auth.domain.{Regimes, UserAuthority}
import uk.gov.hmrc.common.microservice.sa.domain.SaRoot
import uk.gov.hmrc.domain.SaUtr

class SaControllerSpec extends BaseSpec {

  private def saRoot = Some(SaRoot(SaUtr("sa-utr"), Map.empty[String, String]))

  "render the Make a Payment page" in new WithApplication(FakeApplication()) with PortalUrlBuilderMock  {

    val controllerUnderTest = new SaController with MockedPortalUrlBuilder

    implicit val user = User(userId = "userId", userAuthority = UserAuthority("userId", Regimes()), nameFromGovernmentGateway = Some("Ciccio"), regimes = RegimeRoots(sa = saRoot), decryptedToken = None)
    implicit val request = FakeRequest()

    when(mockPortalUrlBuilder.buildPortalUrl("btDirectDebits")).thenReturn("saDirectDebitsUrl")

    val result = Future.successful(controllerUnderTest.makeAPaymentPage(user, request))

    status(result) shouldBe 200
    verify(mockPortalUrlBuilder).buildPortalUrl("btDirectDebits")
  }


}


