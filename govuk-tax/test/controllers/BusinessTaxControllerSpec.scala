package controllers

import test.BaseSpec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.mock.MockitoSugar
import play.api.test.{ FakeRequest, WithApplication }
import play.api.test.FakeApplication
import microservices.MockMicroServicesForTests
import microservice.auth.AuthMicroService
import org.mockito.Mockito._
import scala.Some
import microservice.auth.domain.{ Regimes, UserAuthority }
import java.net.URI
import org.joda.time.DateTime
import microservice.sa.SaMicroService
import play.api.test.Helpers._
import microservice.sa.domain.{ SaIndividualAddress, SaPerson, SaRoot }

class BusinessTaxControllerSpec extends BaseSpec with ShouldMatchers with MockitoSugar with CookieEncryption {

  private val mockAuthMicroService = mock[AuthMicroService]
  private val mockSaMicroService = mock[SaMicroService]

  private def controller = new BusinessTaxController with MockMicroServicesForTests {
    override val authMicroService = mockAuthMicroService
    override val saMicroService = mockSaMicroService
  }

  val saName = "Geoff Fisher From SA"
  val ggwName = "Geoffrey From GGW"

  when(mockAuthMicroService.authority("/auth/oid/gfisher")).thenReturn(
    Some(UserAuthority("someIdWeDontCareAboutHere", Regimes(paye = Some(URI.create("/personal/paye/DF334476B")), sa = Some(URI.create("/personal/sa/123456789012")), vat = Set(URI.create("/some-undecided-url"))), Some(new DateTime(1000L)))))

  when(mockSaMicroService.root("/personal/sa/123456789012")).thenReturn(
    SaRoot(
      utr = "123456789012",
      links = Map(
        "personalDetails" -> "/personal/sa/123456789012/details")
    )
  )

  "The home method" should {

    "display both the Government Gateway name and CESA/SA name for Geoff Fisher and a link to his individual SA address" in new WithApplication(FakeApplication()) {

      when(mockSaMicroService.person("/personal/sa/123456789012/details")).thenReturn(
        Some(SaPerson(
          name = saName,
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

      val result = controller.home(FakeRequest().withSession("userId" -> encrypt("/auth/oid/gfisher"), "ggwName" -> ggwName))

      status(result) should be(200)

      val content = contentAsString(result)

      content should include(ggwName)
      content should include("Self-assessment (SA)</a>")
      content should include("href=\"/sa/details\"")
      content should include("Value Added Tax (VAT)</a>")
      content should include("href=\"#\"")

    }

    "display an error page if personal details do not come back from backend service" in new WithApplication(FakeApplication()) {

      when(mockSaMicroService.person("/personal/sa/123456789012/details")).thenReturn(None)
      val result = controller.home(FakeRequest().withSession(("userId", encrypt("/auth/oid/gfisher"))))

      status(result) should be(404)

    }

  }

}
