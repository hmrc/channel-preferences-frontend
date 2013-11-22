package controllers.bt

import play.api.test.{FakeRequest, WithApplication}
import controllers.bt.testframework.mocks.PortalUrlBuilderMock
import uk.gov.hmrc.common.microservice.sa.domain.SaRoot
import org.mockito.Mockito._
import uk.gov.hmrc.common.BaseSpec
import play.api.test.Helpers._
import uk.gov.hmrc.common.microservice.auth.domain.UserAuthority
import uk.gov.hmrc.common.microservice.auth.domain.Regimes
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.common.microservice.domain.RegimeRoots
import play.api.test.FakeApplication
import scala.Some

class MergeGGAccountsControllerSpec extends BaseSpec {

  "render the Merge Government Gateway Accounts page" should {

    "return the merge government gateway accounts page succesfully" in new WithApplication(FakeApplication()) with PortalUrlBuilderMock {

      val controllerUnderTest = new MergeGGAccountsController with MockedPortalUrlBuilder

      val saRoot = Some(SaRoot(SaUtr("sa-utr"), Map.empty[String, String]))
      val user = User(userId = "userId", userAuthority = UserAuthority("userId", Regimes()), nameFromGovernmentGateway = Some("Ciccio"), regimes = RegimeRoots(sa = saRoot), decryptedToken = None)
      val request = FakeRequest()

      when(mockPortalUrlBuilder.buildPortalUrl("servicesDeEnrolment")).thenReturn("servicesDeEnrolmentUrl")
      when(mockPortalUrlBuilder.buildPortalUrl("otherServicesEnrolment")).thenReturn("otherServicesEnrolmentUrl")

      val result = controllerUnderTest.mergeGGAccountsPage(user, request)


      status(result) shouldBe 200
      verify(mockPortalUrlBuilder).buildPortalUrl("servicesDeEnrolment")
      verify(mockPortalUrlBuilder).buildPortalUrl("otherServicesEnrolment")
    }
  }


}
