package controllers

import test.BaseSpec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.mock.MockitoSugar
import play.api.test.{ FakeRequest, WithApplication }
import play.api.test.FakeApplication
import microservice.auth.AuthMicroService
import org.mockito.Mockito._
import scala.Some
import microservice.auth.domain.{ Vrn, Utr, Regimes, UserAuthority }
import java.net.URI
import org.joda.time.DateTime
import microservice.sa.SaMicroService
import play.api.test.Helpers._
import microservice.sa.domain.{ SaIndividualAddress, SaPerson, SaRoot }
import microservice.MockMicroServicesForTests
import controllers.SessionTimeoutWrapper._
import microservice.auth.domain.UserAuthority
import microservice.sa.domain.SaRoot
import microservice.sa.domain.SaIndividualAddress
import microservice.auth.domain.Utr
import scala.Some
import microservice.auth.domain.Vrn
import microservice.auth.domain.Regimes
import microservice.sa.domain.SaPerson
import play.api.test.FakeApplication

class BusinessTaxControllerSpec extends BaseSpec with ShouldMatchers with MockitoSugar with CookieEncryption {

  private val mockAuthMicroService = mock[AuthMicroService]
  private val mockSaMicroService = mock[SaMicroService]

  private def controller = new BusinessTaxController with MockMicroServicesForTests {
    override val authMicroService = mockAuthMicroService
    override val saMicroService = mockSaMicroService
  }

  val nameFromSa = "Geoff Fisher From SA"
  val nameFromGovernmentGateway = "Geoffrey From Government Gateway"
  val encodedGovernmentGatewayToken = "someEncodedToken"

  when(mockSaMicroService.root("/personal/sa/123456789012")).thenReturn(
    SaRoot(
      utr = "123456789012",
      links = Map(
        "personalDetails" -> "/personal/sa/123456789012/details")
    )
  )

  "The home method" should {

    "display both the Government Gateway name and CESA/SA name for Geoff Fisher and a link to details page of the regimes he has actively enrolled online services for (SA and VAT here)" in new WithApplication(FakeApplication()) {

      val utr = Utr("1234567890")
      val vrn = Vrn("666777889")
      when(mockAuthMicroService.authorityFromOid("gfisher")).thenReturn(
        Some(UserAuthority("someIdWeDontCareAboutHere", Regimes(paye = Some(URI.create("/personal/paye/DF334476B")), sa = Some(URI.create("/personal/sa/123456789012")), vat = Set(URI.create("/some-undecided-url"))), Some(new DateTime(1000L)), utr = Some(utr), vrn = Some(vrn))))

      when(mockSaMicroService.person("/personal/sa/123456789012/details")).thenReturn(
        Some(SaPerson(
          name = nameFromSa,
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

      val result = controller.home(FakeRequest().withSession("userId" -> encrypt("gfisher"), "name" -> encrypt(nameFromGovernmentGateway), "token" -> encrypt(encodedGovernmentGatewayToken), sessionTimestampKey -> controller.now().getMillis.toString))

      status(result) should be(200)

      val content = contentAsString(result)

      content should include(nameFromGovernmentGateway)
      content should include("UTR: " + utr)
      content should include("VRN: " + vrn)
      content should include("Self-assessment (SA)</a>")
      content should include("href=\"/sa/details\"")
      content should include("Value Added Tax (VAT)</a>")
      content should include("href=\"#\"")

    }

    "display the Government Gateway name for Geoff Fisher and a respective notice if he is not actively enrolled for any online services" in new WithApplication(FakeApplication()) {

      when(mockAuthMicroService.authorityFromOid("gfisher")).thenReturn(
        Some(UserAuthority("someIdWeDontCareAboutHere", Regimes(paye = None, sa = None, vat = Set()), Some(new DateTime(1000L)))))

      val result = controller.home(FakeRequest().withSession("userId" -> encrypt("gfisher"), "name" -> encrypt(nameFromGovernmentGateway), "token" -> encrypt(encodedGovernmentGatewayToken), sessionTimestampKey -> controller.now().getMillis.toString))

      status(result) should be(200)

      val content = contentAsString(result)

      content should include("You are not currently actively enrolled for any online services")
      content should not include ("Self-assessment (SA)</a>")
      content should not include ("Value Added Tax (VAT)</a>")

    }
  }
}
