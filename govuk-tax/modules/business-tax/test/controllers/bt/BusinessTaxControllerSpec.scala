package controllers.bt

import play.api.test.{ FakeRequest, WithApplication }
import uk.gov.hmrc.common.microservice.auth.AuthMicroService
import java.net.URI
import org.joda.time.DateTime
import uk.gov.hmrc.common.microservice.sa.SaConnector
import play.api.test.Helpers._
import controllers.common.SessionTimeoutWrapper._
import uk.gov.hmrc.common.BaseSpec
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import controllers.common.CookieEncryption
import uk.gov.hmrc.common.microservice.auth.domain.UserAuthority
import uk.gov.hmrc.common.microservice.sa.domain.{SaName, SaRoot, SaIndividualAddress, SaPerson}
import uk.gov.hmrc.common.microservice.auth.domain.Regimes
import play.api.test.FakeApplication
import uk.gov.hmrc.common.microservice.vat.VatConnector
import uk.gov.hmrc.common.microservice.vat.domain.VatDomain.{ VatAccountBalance, VatAccountSummary, VatRoot }
import play.api.i18n.Messages
import config.DateTimeProvider
import uk.gov.hmrc.common.microservice.paye.PayeMicroService
import uk.gov.hmrc.domain.{SaUtr, Vrn}
import java.util.UUID
import uk.gov.hmrc.common.microservice.epaye.EPayeConnector
import controllers.bt.regimeViews.{AccountSummariesFactory, VatMessageKeys}
import uk.gov.hmrc.common.microservice.ct.CtConnector

class BusinessTaxControllerSpec extends BaseSpec with MockitoSugar with CookieEncryption {

  import uk.gov.hmrc.common.MockUtils._

  private lazy val mockAuthMicroService = mock[AuthMicroService]
  private lazy val mockSaConnector = mock[SaConnector]
  private lazy val mockVatConnector = mock[VatConnector]
  private lazy val mockPayeMicroService = mock[PayeMicroService]
  private lazy val mockCtConnector = mock[CtConnector]
  private lazy val mockEPayeConnector = mock[EPayeConnector]

  private def dateTime = () => DateTimeProvider.now()
  private def sessionTimeout : String = dateTime().getMillis.toString

  private def controller = new BusinessTaxController(new AccountSummariesFactory(mockSaConnector, mockVatConnector, mockCtConnector, mockEPayeConnector)) {
    override lazy val payeMicroService = mockPayeMicroService
    override lazy val saConnector = mockSaConnector
    override lazy val vatConnector = mockVatConnector
    override lazy val authMicroService = mockAuthMicroService
    override lazy val ctConnector = mockCtConnector
    override lazy val epayeConnector = mockEPayeConnector
  }

  private val nameFromSa = SaName("Mr.", "Geoff", None, "Fisher", Some("From SA"))

  val nameFromGovernmentGateway = "Geoffrey From Government Gateway"
  val encodedGovernmentGatewayToken = "someEncodedToken"

  when(mockSaConnector.root("/sa/individual/123456789012")).thenReturn(
    SaRoot(
      utr = "123456789012",
      links = Map(
        "individual/details" -> "/sa/individual/123456789012/details")
    )
  )

  before {
    resetAll(mockAuthMicroService, mockSaConnector, mockVatConnector, mockEPayeConnector)
  }

  "The home method" should {

    "display both the Government Gateway name for Geoff Fisher and a link to details page of the regimes he has actively enrolled online services for (SA and VAT here)" in new WithApplication(FakeApplication()) {

      val utr = SaUtr("1234567890")
      val vrn = Vrn("666777889")

      when(mockAuthMicroService.authority("/auth/oid/gfisher")).thenReturn(
        Some(UserAuthority("someIdWeDontCareAboutHere", Regimes(
              sa = Some(URI.create("/sa/individual/123456789012")),
              vat = Some(URI.create("/vat/754645112"))), Some(new DateTime(1000L)), saUtr = Some(utr), vrn = Some(vrn))))

      when(mockSaConnector.person("/sa/individual/123456789012/details")).thenReturn(
        Some(SaPerson(
          name = nameFromSa,
          utr = "123456789012",
          address = SaIndividualAddress(
            addressLine1 = "address line 1",
            addressLine2 = "address line 2",
            addressLine3 = Some("address line 3"),
            addressLine4 = Some("address line 4"),
            addressLine5 = Some("address line 5"),
            postcode = Some("postcode"),
            foreignCountry = Some("foreign country"),
            additionalDeliveryInformation = Some("additional delivery information")
          )
        ))
      )

      when(mockVatConnector.root("/vat/754645112")).thenReturn(VatRoot(Vrn("754645112"), Map.empty))

      val result = controller.home(FakeRequest().withSession("sessionId" -> encrypt(s"session-${UUID.randomUUID().toString}"), "userId" -> encrypt("/auth/oid/gfisher"), "name" -> encrypt(nameFromGovernmentGateway), "token" -> encrypt(encodedGovernmentGatewayToken),
        sessionTimestampKey -> sessionTimeout, "affinityGroup" -> encrypt("someaffinitygroup")))

      status(result) should be(200)

      val content = contentAsString(result)

      content should include(nameFromGovernmentGateway)
      content should include("<h2>Self-Assessment</h2>")
      content should include("href=\"/sa/home\"")
      content should include("<h2>VAT</h2>")
    }

    "display the account balance of a user enrolled for VAT" in new WithApplication(FakeApplication()) {
      val vrn = "12345678"
      val date = "2012-06-06"
      val accountSummary = VatAccountSummary(Some(VatAccountBalance(Some(6.1), Some("GBP"))), Some(date))

      when(mockAuthMicroService.authority("/auth/oid/johnboy")).thenReturn(
        Some(UserAuthority("someIdWeDontCareAboutHere", Regimes(vat = Some(new URI(s"/vat/$vrn"))), Some(new DateTime(1000L)), None, Some(Vrn(vrn)))))

      when(mockVatConnector.root(s"/vat/$vrn")).thenReturn(VatRoot(Vrn(vrn), Map("accountSummary" -> s"/vat/$vrn/accountSummary")))

      when(mockVatConnector.accountSummary(s"/vat/$vrn/accountSummary")).thenReturn(Some(accountSummary))

      val result = controller.home(FakeRequest().withSession("sessionId" -> encrypt(s"session-${UUID.randomUUID().toString}"), "userId" -> encrypt("/auth/oid/johnboy"), "name" -> encrypt(nameFromGovernmentGateway), "token" -> encrypt(encodedGovernmentGatewayToken),
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
        Some(UserAuthority("someIdWeDontCareAboutHere", Regimes(vat = Some(new URI(s"/vat/$vrn"))), Some(new DateTime(1000L)), None, Some(Vrn(vrn)))))

      when(mockVatConnector.root(s"/vat/$vrn")).thenReturn(VatRoot(Vrn(vrn), Map("accountSummary" -> s"/vat/$vrn/accountSummary")))

      when(mockVatConnector.accountSummary(s"/vat/$vrn/accountSummary")).thenReturn(Some(accountSummary))

      val result = controller.home(FakeRequest().withSession("sessionId" -> encrypt(s"session-${UUID.randomUUID().toString}"), "userId" -> encrypt("/auth/oid/johnboy"), "name" -> encrypt(nameFromGovernmentGateway), "token" -> encrypt(encodedGovernmentGatewayToken),
        sessionTimestampKey -> sessionTimeout, "affinityGroup" -> encrypt("someaffinitygroup")))

      status(result) should be(200)
      val content = contentAsString(result)
      content should include(Messages(VatMessageKeys.vatSummaryUnavailableErrorMessage1))
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
