package controllers.bt

import play.api.test.{ FakeRequest, WithApplication }
import uk.gov.hmrc.microservice.auth.AuthMicroService
import java.net.URI
import org.joda.time.DateTime
import uk.gov.hmrc.microservice.sa.SaMicroService
import play.api.test.Helpers._
import uk.gov.hmrc.microservice.MockMicroServicesForTests
import controllers.common.SessionTimeoutWrapper._
import uk.gov.hmrc.microservice.auth.domain._
import uk.gov.hmrc.common.BaseSpec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import controllers.common.CookieEncryption
import uk.gov.hmrc.microservice.auth.domain.UserAuthority
import uk.gov.hmrc.microservice.sa.domain.SaRoot
import uk.gov.hmrc.microservice.sa.domain.SaIndividualAddress
import uk.gov.hmrc.microservice.auth.domain.Utr
import scala.Some
import uk.gov.hmrc.microservice.auth.domain.Vrn
import uk.gov.hmrc.microservice.auth.domain.Regimes
import uk.gov.hmrc.microservice.sa.domain.SaPerson
import play.api.test.FakeApplication

class BusinessTaxControllerSpec extends BaseSpec with MockitoSugar with CookieEncryption {

  private lazy val mockAuthMicroService = mock[AuthMicroService]
  private lazy val mockSaMicroService = mock[SaMicroService]

  private def controller = new BusinessTaxController with MockMicroServicesForTests {
    override lazy val authMicroService = mockAuthMicroService
    override lazy val saMicroService = mockSaMicroService
  }

  val nameFromSa = "Geoff Fisher From SA"
  val nameFromGovernmentGateway = "Geoffrey From Government Gateway"
  val encodedGovernmentGatewayToken = "someEncodedToken"

  when(mockSaMicroService.root("/sa/individual/123456789012")).thenReturn(
    SaRoot(
      utr = "123456789012",
      links = Map(
        "individual/details" -> "/sa/individual/123456789012/details")
    )
  )

  "The home method" should {

    "display both the Government Gateway name and CESA/SA name for Geoff Fisher and a link to details page of the regimes he has actively enrolled online services for (SA and VAT here)" in new WithApplication(FakeApplication()) {

      val utr = Utr("1234567890")
      val vrn = Vrn("666777889")
      when(mockAuthMicroService.authority("/auth/oid/gfisher")).thenReturn(
        Some(UserAuthority("someIdWeDontCareAboutHere", Regimes(paye = Some(URI.create("/personal/paye/DF334476B")), sa = Some(URI.create("/sa/individual/123456789012")), vat = Set(URI.create("/some-undecided-url"))), Some(new DateTime(1000L)), utr = Some(utr), vrn = Some(vrn))))

      when(mockSaMicroService.person("/sa/individual/123456789012/home")).thenReturn(
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
            additionalDeliveryInformation = "additional delivery information"
          )
        ))
      )

      val result = controller.home(FakeRequest().withSession("userId" -> encrypt("/auth/oid/gfisher"), "name" -> encrypt(nameFromGovernmentGateway), "token" -> encrypt(encodedGovernmentGatewayToken),
        sessionTimestampKey -> controller.now().getMillis.toString, "affinityGroup" -> encrypt("someaffinitygroup")))

      status(result) should be(200)

      val content = contentAsString(result)

      content should include(nameFromGovernmentGateway)
      content should include("UTR: " + utr)
      content should include("VRN: " + vrn)
      content should include("Self-assessment (SA)</a>")
      content should include("href=\"/sa/home\"")
      content should include("Value Added Tax (VAT)</a>")
      content should include("href=\"#\"")

    }

    "display the Government Gateway name for Geoff Fisher and a respective notice if he is not actively enrolled for any online services" in new WithApplication(FakeApplication()) {

      when(mockAuthMicroService.authority("/auth/oid/gfisher")).thenReturn(
        Some(UserAuthority("someIdWeDontCareAboutHere", Regimes(paye = None, sa = None, vat = Set()), Some(new DateTime(1000L)))))

      val result = controller.home(FakeRequest().withSession("userId" -> encrypt("/auth/oid/gfisher"), "name" -> encrypt(nameFromGovernmentGateway), "token" -> encrypt(encodedGovernmentGatewayToken),
        sessionTimestampKey -> controller.now().getMillis.toString, "affinityGroup" -> encrypt("someaffinitygroup")))

      status(result) should be(200)

      val content = contentAsString(result)

      content should include("You are not currently actively enrolled for any online services")
      content should not include "Self-assessment (SA)</a>"
      content should not include "Value Added Tax (VAT)</a>"

    }

    "display the CT UTR of Geoff Fisher if he is enrolled for the CT service" in new WithApplication(FakeApplication()) {

      val ctUtr = Utr("ct utr 1234567890")
      when(mockAuthMicroService.authority("/auth/oid/gfisher")).thenReturn(
        Some(UserAuthority("someIdWeDontCareAboutHere", Regimes(), Some(new DateTime(1000L)), ctUtr = Some(ctUtr))))

      val result = controller.home(FakeRequest().withSession("userId" -> encrypt("/auth/oid/gfisher"), "name" -> encrypt(nameFromGovernmentGateway), "token" -> encrypt(encodedGovernmentGatewayToken),
        sessionTimestampKey -> controller.now().getMillis.toString, "affinityGroup" -> encrypt("someaffinitygroup")))

      status(result) should be(200)

      val content = contentAsString(result)

      content should include(nameFromGovernmentGateway)
      content should include("CT UTR: " + ctUtr)
    }

    "display the Employer Reference of Geoff Fisher if he is enrolled for the PAYE service" in new WithApplication(FakeApplication()) {

      val empRef = EmpRef("taxRef", "taxNum")
      when(mockAuthMicroService.authority("/auth/oid/gfisher")).thenReturn(
        Some(UserAuthority("someIdWeDontCareAboutHere", Regimes(), Some(new DateTime(1000L)), empRef = Some(empRef))))

      val result = controller.home(FakeRequest().withSession("userId" -> encrypt("/auth/oid/gfisher"), "name" -> encrypt(nameFromGovernmentGateway), "token" -> encrypt(encodedGovernmentGatewayToken),
        sessionTimestampKey -> controller.now().getMillis.toString, "affinityGroup" -> encrypt("someaffinitygroup")))

      status(result) should be(200)

      val content = contentAsString(result)

      content should include(nameFromGovernmentGateway)
      content should include("Employer Reference: " + empRef)
    }

  }
}
