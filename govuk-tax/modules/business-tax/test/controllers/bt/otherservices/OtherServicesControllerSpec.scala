package controllers.bt.otherservices

import uk.gov.hmrc.common.BaseSpec
import org.mockito.Mockito._
import play.api.test.Helpers._
import play.api.test.{FakeRequest, FakeApplication, WithApplication}
import controllers.bt.testframework.mocks.PortalUrlBuilderMock
import controllers.bt.OtherServicesController
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.common.microservice.sa.domain.SaRoot
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.common.microservice.domain.{RegimeRoots, User}
import uk.gov.hmrc.common.microservice.auth.domain.{Regimes, UserAuthority}
import org.jsoup.Jsoup
import uk.gov.hmrc.common.microservice.governmentgateway.{ProfileResponse, GovernmentGatewayConnector}
import uk.gov.hmrc.common.microservice.epaye.domain.EpayeRoot
import uk.gov.hmrc.common.microservice.vat.domain.VatRoot

class OtherServicesControllerSpec extends BaseSpec with MockitoSugar {

  "Calling otherservices with a valid logged in business user" should {

    "render the Other Services template including the three sections if all the summaries are available " in new WithApplication(FakeApplication()) with PortalUrlBuilderMock {

      val ggw = mock[GovernmentGatewayConnector]
      val controllerUnderTest = new OtherServicesController(ggw, null)(null)

      val saRoot = Some(SaRoot(SaUtr("sa-utr"), Map.empty[String, String]))
      val user = User(userId = "userId", userAuthority = UserAuthority("userId", Regimes()), nameFromGovernmentGateway = Some("Ciccio"), regimes = RegimeRoots(sa = saRoot), decryptedToken = None)
      val request = FakeRequest()

      when(ggw.profile("userId")).thenReturn(ProfileResponse("Individual", List("HMCE-ECSL-ORG", "HMRC-EU-REF-ORG", "hmce-vatrsl-org")))

      val result = controllerUnderTest.otherServicesPage(user, request)

      status(result) shouldBe 200
      println(contentAsString(result))
      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("manageYourTaxesSection") should not be null
      document.getElementById("availableServices") should not be null
      document.getElementById("noOtherServices") shouldBe null
    }

    "render the Other Services template with the generic registration message if the registration link is not available" in new WithApplication(FakeApplication()) with PortalUrlBuilderMock {

      val ggw = mock[GovernmentGatewayConnector]
      val controllerUnderTest = new OtherServicesController(ggw, null)(null)

      val saRoot = Some(SaRoot(SaUtr("sa-utr"), Map.empty[String, String]))
      val epaye = Some(mock[EpayeRoot])
      val vat = Some(mock[VatRoot])
      val user = User(userId = "userId", userAuthority = UserAuthority("userId", Regimes()), nameFromGovernmentGateway = Some("Ciccio"), regimes = RegimeRoots(sa = saRoot, vat = vat, epaye = epaye), decryptedToken = None)

      when(ggw.profile("userId")).thenReturn(ProfileResponse("Individual", List("HMCE-ECSL-ORG", "HMRC-EU-REF-ORG", "hmce-vatrsl-org")))

      val result = controllerUnderTest.otherServicesPage(user, FakeRequest())

      status(result) shouldBe 200

      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("registrationGenericMessage") should not be null
      document.getElementById("businessTaxesRegistrationLink") shouldBe null

    }
  }
}
