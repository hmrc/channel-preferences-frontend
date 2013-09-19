package controllers.bt

import play.api.test.{ FakeRequest, WithApplication }
import uk.gov.hmrc.common.microservice.auth.AuthMicroService
import java.net.URI
import org.joda.time.DateTime
import uk.gov.hmrc.common.microservice.sa.SaMicroService
import play.api.test.Helpers._
import controllers.common.SessionTimeoutWrapper._
import uk.gov.hmrc.common.BaseSpec
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import controllers.common.CookieEncryption
import uk.gov.hmrc.common.microservice.auth.domain.UserAuthority
import uk.gov.hmrc.common.microservice.sa.domain.SaRoot
import uk.gov.hmrc.common.microservice.sa.domain.SaIndividualAddress
import uk.gov.hmrc.common.microservice.auth.domain.Regimes
import uk.gov.hmrc.common.microservice.sa.domain.SaPerson
import play.api.test.FakeApplication
import uk.gov.hmrc.common.microservice.vat.VatMicroService
import uk.gov.hmrc.common.microservice.vat.domain.VatDomain.{ VatAccountBalance, VatAccountSummary, VatRoot }
import play.api.i18n.Messages
import config.DateTimeProvider
import uk.gov.hmrc.common.microservice.paye.PayeMicroService
import uk.gov.hmrc.common.microservice.paye.domain.PayeRoot
import uk.gov.hmrc.domain.{SaUtr, EmpRef, Vrn, CtUtr}

class BusinessTaxControllerSpec extends BaseSpec with MockitoSugar with CookieEncryption {

  import uk.gov.hmrc.common.MockUtils._

  private lazy val mockAuthMicroService = mock[AuthMicroService]
  private lazy val mockSaMicroService = mock[SaMicroService]
  private lazy val mockVatMicroService = mock[VatMicroService]
  private lazy val mockPayeMicroService = mock[PayeMicroService]

  private def dateTime = () => DateTimeProvider.now()
  private def sessionTimeout : String = dateTime().getMillis.toString

  private def controller = new BusinessTaxController(new AccountSummariesFactory(mockSaMicroService, mockVatMicroService)) {
    override lazy val payeMicroService = mockPayeMicroService
    override lazy val saMicroService = mockSaMicroService
    override lazy val vatMicroService = mockVatMicroService
    override lazy val authMicroService = mockAuthMicroService
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

  before {
    resetAll(mockAuthMicroService, mockSaMicroService, mockVatMicroService)
  }

  "The home method" should {

    "display both the Government Gateway name and CESA/SA name for Geoff Fisher and a link to details page of the regimes he has actively enrolled online services for (SA and VAT here)" in new WithApplication(FakeApplication()) {

      val utr = SaUtr("1234567890")
      val vrn = Vrn("666777889")

      when(mockPayeMicroService.root("/personal/paye/DF334476B")).thenReturn(PayeRoot("1112234",1,"title","firstName",None,"surname","name","1976-13-04",Map.empty,Map.empty))
      when(mockAuthMicroService.authority("/auth/oid/gfisher")).thenReturn(
        Some(UserAuthority("someIdWeDontCareAboutHere", Regimes(paye = Some(URI.create("/personal/paye/DF334476B")), sa = Some(URI.create("/sa/individual/123456789012")), vat = Some(URI.create("/vat/vrn/754645112"))), Some(new DateTime(1000L)), saUtr = Some(utr), vrn = Some(vrn))))

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

      when(mockVatMicroService.root("/vat/vrn/754645112")).thenReturn(VatRoot(Vrn("754645112"), Map.empty))

      val result = controller.home(FakeRequest().withSession("userId" -> encrypt("/auth/oid/gfisher"), "name" -> encrypt(nameFromGovernmentGateway), "token" -> encrypt(encodedGovernmentGatewayToken),
        sessionTimestampKey -> sessionTimeout, "affinityGroup" -> encrypt("someaffinitygroup")))

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
        Some(UserAuthority("someIdWeDontCareAboutHere", Regimes(paye = None, sa = None, vat = None), Some(new DateTime(1000L)))))

      val result = controller.home(FakeRequest().withSession("userId" -> encrypt("/auth/oid/gfisher"), "name" -> encrypt(nameFromGovernmentGateway), "token" -> encrypt(encodedGovernmentGatewayToken),
        sessionTimestampKey -> sessionTimeout, "affinityGroup" -> encrypt("someaffinitygroup")))

      status(result) should be(200)

      val content = contentAsString(result)

      content should include("You are not currently actively enrolled for any online services")
      content should not include "Self-assessment (SA)</a>"
      content should not include "Value Added Tax (VAT)</a>"

    }

    "display the CT UTR of Geoff Fisher if he is enrolled for the CT service" in new WithApplication(FakeApplication()) {

      val ctUtr = CtUtr("ct utr 1234567890")
      when(mockAuthMicroService.authority("/auth/oid/gfisher")).thenReturn(
        Some(UserAuthority("someIdWeDontCareAboutHere", Regimes(), Some(new DateTime(1000L)), ctUtr = Some(ctUtr))))

      val result = controller.home(FakeRequest().withSession("userId" -> encrypt("/auth/oid/gfisher"), "name" -> encrypt(nameFromGovernmentGateway), "token" -> encrypt(encodedGovernmentGatewayToken),
        sessionTimestampKey -> sessionTimeout, "affinityGroup" -> encrypt("someaffinitygroup")))

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
        sessionTimestampKey -> sessionTimeout, "affinityGroup" -> encrypt("someaffinitygroup")))

      status(result) should be(200)

      val content = contentAsString(result)

      content should include(nameFromGovernmentGateway)
      content should include("Employer Reference: " + empRef)
    }

    "display the account balance of a user enrolled for VAT" in new WithApplication(FakeApplication()) {
      val vrn = "12345678"
      val date = "2012-06-06"
      val accountSummary = VatAccountSummary(Some(VatAccountBalance(Some(6.1), Some("GBP"))), Some(date))

      when(mockAuthMicroService.authority("/auth/oid/johnboy")).thenReturn(
        Some(UserAuthority("someIdWeDontCareAboutHere", Regimes(vat = Some(new URI(s"/vat/vrn/$vrn"))), Some(new DateTime(1000L)), None, Some(Vrn(vrn)))))

      when(mockVatMicroService.root(s"/vat/vrn/$vrn")).thenReturn(VatRoot(Vrn(vrn), Map("accountSummary" -> s"/vat/vrn/$vrn/accountSummary")))

      when(mockVatMicroService.accountSummary(s"/vat/vrn/$vrn/accountSummary")).thenReturn(Some(accountSummary))

      val result = controller.home(FakeRequest().withSession("userId" -> encrypt("/auth/oid/johnboy"), "name" -> encrypt(nameFromGovernmentGateway), "token" -> encrypt(encodedGovernmentGatewayToken),
        sessionTimestampKey -> sessionTimeout, "affinityGroup" -> encrypt("someaffinitygroup")))

      status(result) should be(200)

      val content = contentAsString(result)
      content should include("6.1")
      content should include(vrn)
    }

    "display error text if the account balance is unavailable" in new WithApplication(FakeApplication()) {
      val vrn = "12345678"
      val date = "2012-06-06"
      val accountSummary = VatAccountSummary(None, Some(date))

      when(mockAuthMicroService.authority("/auth/oid/johnboy")).thenReturn(
        Some(UserAuthority("someIdWeDontCareAboutHere", Regimes(vat = Some(new URI(s"/vat/vrn/$vrn"))), Some(new DateTime(1000L)), None, Some(Vrn(vrn)))))

      when(mockVatMicroService.root(s"/vat/vrn/$vrn")).thenReturn(VatRoot(Vrn(vrn), Map("accountSummary" -> s"/vat/vrn/$vrn/accountSummary")))

      when(mockVatMicroService.accountSummary(s"/vat/vrn/$vrn/accountSummary")).thenReturn(Some(accountSummary))

      val result = controller.home(FakeRequest().withSession("userId" -> encrypt("/auth/oid/johnboy"), "name" -> encrypt(nameFromGovernmentGateway), "token" -> encrypt(encodedGovernmentGatewayToken),
        sessionTimestampKey -> sessionTimeout, "affinityGroup" -> encrypt("someaffinitygroup")))

      status(result) should be(200)
      val content = contentAsString(result)
      content should include(Messages("vat.error.message.summaryUnavailable.1"))
    }

  }

  "Make a payment landing page " should {
    "Render some make a payment text when a user is logged in" in new WithApplication(FakeApplication()) {
      val result = controller.makeAPaymentLandingAction()

      status(result) should be(200)

      val htmlBody = contentAsString(result)
      htmlBody should include("Make a payment landing page")
    }
  }

}
