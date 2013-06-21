package controllers

import test.BaseSpec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.mock.MockitoSugar
import play.api.test.{ FakeRequest, WithApplication }
import microservices.MockMicroServicesForTests
import microservice.auth.AuthMicroService
import org.mockito.Mockito._
import microservice.sa.domain._
import microservice.auth.domain.UserAuthority
import play.api.test.FakeApplication
import scala.Some
import play.api.mvc.Cookie
import microservice.sa.SaMicroService

class SaControllerSpec extends BaseSpec with ShouldMatchers with MockitoSugar {

  import play.api.test.Helpers._

  private val mockAuthMicroService = mock[AuthMicroService]

  when(mockAuthMicroService.authority("/auth/oid/gfisher")).thenReturn(
    Some(UserAuthority("someIdWeDontCareAboutHere", Map("paye" -> "/personal/paye/DF334476B", "sa" -> "/personal/sa/123456789012"))))

  private val mockSaMicroService = mock[SaMicroService]

  when(mockSaMicroService.root("/personal/sa/123456789012")).thenReturn(
    SaRoot(
      utr = "123456789012",
      links = Map(
        "personalDetails" -> "/personal/sa/123456789012/personalDetails")
    )
  )

  when(mockSaMicroService.person("/personal/sa/123456789012/personalDetails")).thenReturn(
    Some(SaPerson(
      name = "Geoff Fisher",
      utr = "123456789012",
      address = SaIndividualAddress(
        addressLine1 = "address line 1",
        addressLine2 = "address line 2",
        addressLine3 = "address line 3",
        addressLine4 = "address line 4",
        addressLine5 = "address line 5",
        postcode = "postcode",
        foreignCountry = "foreign country",
        additionalDeliveryInformation = "additional delivery info"
      )
    ))
  )

  private def controller = new SaController with MockMicroServicesForTests {
    override val authMicroService = mockAuthMicroService
    override val saMicroService = mockSaMicroService
  }

  "The home method" should {

    "display the name for Geoff Fisher" in new WithApplication(FakeApplication()) {
      val content = requestHome
      content should include("Geoff Fisher")
    }

    "return the link to show the individual SA address of Geoff Fisher" in new WithApplication(FakeApplication()) {
      (pending)
      //      val content = requestHome
      //      content should include("Click here to see your SA address")
    }

    def requestHome: String = {
      val home = controller.home
      val result = home(FakeRequest().withCookies(Cookie("userId", "/auth/oid/gfisher")))

      status(result) should be(200)

      contentAsString(result)
    }
  }

  //  "The benefits method" should {
  //
  //    "display John's benefits" in new WithApplication(FakeApplication()) {
  //      requestBenefits should include("£ 135.33")
  //    }
  //
  //    "not display a benefits without a corresponding employment" in new WithApplication(FakeApplication()) {
  //      requestBenefits should not include "£ 22.22"
  //    }
  //
  //    "display car details" in new WithApplication(FakeApplication()) {
  //      requestBenefits should include("Engine size: 1")
  //      requestBenefits should include("Fuel type: 2")
  //      requestBenefits should include("Date car registered: 12/12/2012")
  //      requestBenefits should include("£ 321.42")
  //    }
  //
  //    def requestBenefits = {
  //      val result = controller.benefits(FakeRequest().withCookies(Cookie("userId", "/auth/oid/jdensmore")))
  //      status(result) shouldBe 200
  //      contentAsString(result)
  //    }
  //
  //  }

}
