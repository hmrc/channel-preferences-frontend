package controllers.bt.otherservices

import uk.gov.hmrc.common.BaseSpec
import views.helpers.LinkMessage
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.domain.EmpRef
import org.scalatest.Matchers
import uk.gov.hmrc.common.microservice.governmentgateway.{GovernmentGatewayConnector, ProfileResponse}
import org.mockito.Mockito._
import uk.gov.hmrc.common.microservice.governmentgateway.AffinityGroupValue._
import play.api.test.{FakeRequest, WithApplication}
import controllers.bt.testframework.mocks.{AffinityGroupParserMock, PortalUrlBuilderMock}
import org.mockito.stubbing.Answer
import org.mockito.invocation.InvocationOnMock
import uk.gov.hmrc.common.microservice.sa.domain.SaRoot
import uk.gov.hmrc.common.microservice.ct.domain.CtRoot
import uk.gov.hmrc.common.microservice.vat.domain.VatRoot
import uk.gov.hmrc.common.microservice.epaye.domain.EpayeRoot
import scala.concurrent._
import org.scalatest.concurrent.ScalaFutures
import scala._
import uk.gov.hmrc.common.microservice.auth.domain.UserAuthority
import uk.gov.hmrc.common.microservice.governmentgateway.AffinityGroup
import uk.gov.hmrc.common.microservice.epaye.domain.EpayeLinks
import views.helpers.RenderableLinkMessage
import scala.Some
import uk.gov.hmrc.domain.Vrn
import uk.gov.hmrc.common.microservice.auth.domain.Regimes
import uk.gov.hmrc.domain.CtUtr
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.common.microservice.domain.RegimeRoots
import play.api.test.FakeApplication
import uk.gov.hmrc.common.microservice.governmentgateway.Enrolment


class OtherServicesFactorySpec extends BaseSpec with MockitoSugar with ScalaFutures {

  "createOnlineServicesEnrolment " should {

    "return an OnlineServicesEnrolment object with the SSO link to access the portal" in new OtherServicesFactoryForTest {

      when(mockPortalUrlBuilder.buildPortalUrl("otherServicesEnrolment")).thenReturn("http://someLink")
      val expected = OnlineServicesEnrolment(RenderableLinkMessage(LinkMessage(href = "http://someLink", text = "online access", sso = true, id = Some("otherServicesEnrolmentHref"))))
      val result = factoryUnderTest.createOnlineServicesEnrolment(mockPortalUrlBuilder.buildPortalUrl)

      result shouldBe expected
    }
  }

  "createOnlineServicesDeEnrolment " should {

    "return an OnlineServicesDeEnrolment object with the SSO link to access the portal" in new OtherServicesFactoryForTest {

      when(mockPortalUrlBuilder.buildPortalUrl("servicesDeEnrolment")).thenReturn("http://someLink")
      val expected = OnlineServicesEnrolment(RenderableLinkMessage(LinkMessage(href = "http://someLink", text = "de-enrol from it", sso = true, id = Some("servicesDeEnrolmentHref"))))
      val result = factoryUnderTest.createOnlineServicesDeEnrolment(mockPortalUrlBuilder.buildPortalUrl)

      result shouldBe expected
    }
  }

  "createBusinessTaxesRegistration" should {
    "return a BusinessTaxRegistration object containing only the link to the hmrc website if epaye, vat and sa are defined" in new OtherServicesFactoryForTest {

      val regimes = RegimeRoots(sa = saRoot, vat = vatRoot, epaye = epayeRoot)

      implicit val user = User("userId", UserAuthority("userId", Regimes()), regimes, decryptedToken = None)

      assertCorrectBusinessTaxRegistration(linkToHmrcWebsite, hmrcWebsiteLinkText)
    }

    "return a BusinessTaxRegistration object containing only the link to the hmrc website if epaye, vat and ct are defined" in new OtherServicesFactoryForTest {

      val regimes = RegimeRoots(ct = ctRoot, vat = vatRoot, epaye = epayeRoot)

      implicit val user = User("userId", UserAuthority("userId", Regimes()), regimes, decryptedToken = None)

      assertCorrectBusinessTaxRegistration(linkToHmrcWebsite, hmrcWebsiteLinkText)
    }

    "return a BusinessTaxRegistration object containing only the link to the hmrc website if epaye, vat, sa and ct are defined" in new OtherServicesFactoryForTest {

      val regimes = RegimeRoots(sa = saRoot, ct = ctRoot, vat = vatRoot, epaye = epayeRoot)

      implicit val user = User("userId", UserAuthority("userId", Regimes()), regimes, decryptedToken = None)

      assertCorrectBusinessTaxRegistration(linkToHmrcWebsite, hmrcWebsiteLinkText)
    }

    "return a BusinessTaxRegistration object containing registration link for all the regimes if none of the regimes are defined" in new OtherServicesFactoryForTest {

      val regimes = RegimeRoots()

      implicit val user = User("userId", UserAuthority("userId", Regimes()), regimes, decryptedToken = None)

      when(mockPortalUrlBuilder.buildPortalUrl("businessRegistration")).thenReturn("http://businessRegistrationLink")

      assertCorrectBusinessTaxRegistration(linkToHmrcOnlineRegistration, "Register for SA, CT, employers PAYE, or VAT")
    }

    "return a BusinessTaxRegistration object containing registration link for sa, ct and epaye regimes if only vat is defined" in new OtherServicesFactoryForTest {

      val regimes = RegimeRoots(vat = vatRoot)

      implicit val user = User("userId", UserAuthority("userId", Regimes()), regimes, decryptedToken = None)

      when(mockPortalUrlBuilder.buildPortalUrl("businessRegistration")).thenReturn("http://businessRegistrationLink")

      assertCorrectBusinessTaxRegistration(linkToHmrcOnlineRegistration, "Register for SA, CT, or employers PAYE")
    }

    "return a BusinessTaxRegistration object containing registration link for sa, ct and vat regimes if only epaye is defined" in new OtherServicesFactoryForTest {

      val regimes = RegimeRoots(epaye = epayeRoot)

      implicit val user = User("userId", UserAuthority("userId", Regimes()), regimes, decryptedToken = None)

      when(mockPortalUrlBuilder.buildPortalUrl("businessRegistration")).thenReturn("http://businessRegistrationLink")

      assertCorrectBusinessTaxRegistration(linkToHmrcOnlineRegistration, "Register for SA, CT, or VAT")
    }

    "return a BusinessTaxRegistration object containing registration link for sa and ct regimes if epaye and vat are defined" in new OtherServicesFactoryForTest {

      val regimes = RegimeRoots(epaye = epayeRoot, vat = vatRoot)

      implicit val user = User("userId", UserAuthority("userId", Regimes()), regimes, decryptedToken = None)

      when(mockPortalUrlBuilder.buildPortalUrl("businessRegistration")).thenReturn("http://businessRegistrationLink")

      assertCorrectBusinessTaxRegistration(linkToHmrcOnlineRegistration, "Register for SA, or CT")
    }

    "return a BusinessTaxRegistration object containing registration link for sa, epaye and vat if only ct is defined" in new OtherServicesFactoryForTest {

      val regimes = RegimeRoots(ct = ctRoot)

      implicit val user = User("userId", UserAuthority("userId", Regimes()), regimes, decryptedToken = None)

      when(mockPortalUrlBuilder.buildPortalUrl("businessRegistration")).thenReturn("http://businessRegistrationLink")

      assertCorrectBusinessTaxRegistration(linkToHmrcOnlineRegistration, "Register for SA, employers PAYE, or VAT")
    }

    "return a BusinessTaxRegistration object containing registration link for sa and epaye if ct and vat are defined" in new OtherServicesFactoryForTest {

      val regimes = RegimeRoots(ct = ctRoot, vat = vatRoot)

      implicit val user = User("userId", UserAuthority("userId", Regimes()), regimes, decryptedToken = None)

      when(mockPortalUrlBuilder.buildPortalUrl("businessRegistration")).thenReturn("http://businessRegistrationLink")

      assertCorrectBusinessTaxRegistration(linkToHmrcOnlineRegistration, "Register for SA, or employers PAYE")
    }

    "return a BusinessTaxRegistration object containing registration link for sa and vat if ct and epaye are defined" in new OtherServicesFactoryForTest {

      val regimes = RegimeRoots(ct = ctRoot, epaye = epayeRoot)

      implicit val user = User("userId", UserAuthority("userId", Regimes()), regimes, decryptedToken = None)

      when(mockPortalUrlBuilder.buildPortalUrl("businessRegistration")).thenReturn("http://businessRegistrationLink")

      assertCorrectBusinessTaxRegistration(linkToHmrcOnlineRegistration, "Register for SA, or VAT")
    }

    "return a BusinessTaxRegistration object containing registration link for ct, epaye and vat if only sa is defined" in new OtherServicesFactoryForTest {

      val regimes = RegimeRoots(sa = saRoot)

      implicit val user = User("userId", UserAuthority("userId", Regimes()), regimes, decryptedToken = None)

      when(mockPortalUrlBuilder.buildPortalUrl("businessRegistration")).thenReturn("http://businessRegistrationLink")

      assertCorrectBusinessTaxRegistration(linkToHmrcOnlineRegistration, "Register for CT, employers PAYE, or VAT")
    }

    "return a BusinessTaxRegistration object containing registration link for ct and epaye if sa and vat are defined" in new OtherServicesFactoryForTest {

      val regimes = RegimeRoots(sa = saRoot, vat = vatRoot)

      implicit val user = User("userId", UserAuthority("userId", Regimes()), regimes, decryptedToken = None)

      when(mockPortalUrlBuilder.buildPortalUrl("businessRegistration")).thenReturn("http://businessRegistrationLink")

      assertCorrectBusinessTaxRegistration(linkToHmrcOnlineRegistration, "Register for CT, or employers PAYE")
    }

    "return a BusinessTaxRegistration object containing registration link for ct and vat if sa and epaye are defined" in new OtherServicesFactoryForTest {

      val regimes = RegimeRoots(sa = saRoot, epaye = epayeRoot)

      implicit val user = User("userId", UserAuthority("userId", Regimes()), regimes, decryptedToken = None)

      when(mockPortalUrlBuilder.buildPortalUrl("businessRegistration")).thenReturn("http://businessRegistrationLink")

      assertCorrectBusinessTaxRegistration(linkToHmrcOnlineRegistration, "Register for CT, or VAT")
    }

    "return a BusinessTaxRegistration object containing registration link for employers paye and vat if sa and ct are defined" in new OtherServicesFactoryForTest {

      val regimes = RegimeRoots(sa = saRoot, ct = ctRoot)

      implicit val user = User("userId", UserAuthority("userId", Regimes()), regimes, decryptedToken = None)

      when(mockPortalUrlBuilder.buildPortalUrl("businessRegistration")).thenReturn("http://businessRegistrationLink")

      assertCorrectBusinessTaxRegistration(linkToHmrcOnlineRegistration, "Register for employers PAYE, or VAT")
    }

    "return a BusinessTaxRegistration object containing registration link for employers paye  if sa, ct and vat are defined" in new OtherServicesFactoryForTest {

      val regimes = RegimeRoots(sa = saRoot, ct = ctRoot, vat = vatRoot)

      implicit val user = User("userId", UserAuthority("userId", Regimes()), regimes, decryptedToken = None)

      when(mockPortalUrlBuilder.buildPortalUrl("businessRegistration")).thenReturn("http://businessRegistrationLink")

      assertCorrectBusinessTaxRegistration(linkToHmrcOnlineRegistration, "Register for employers PAYE")
    }

    "return a BusinessTaxRegistration object containing registration link for vat  if sa, ct and epaye are defined" in new OtherServicesFactoryForTest {

      val regimes = RegimeRoots(sa = saRoot, ct = ctRoot, epaye = epayeRoot)
      implicit val user = User("userId", UserAuthority("userId", Regimes()), regimes, decryptedToken = None)

      when(mockPortalUrlBuilder.buildPortalUrl("businessRegistration")).thenReturn("http://businessRegistrationLink")

      assertCorrectBusinessTaxRegistration(linkToHmrcOnlineRegistration, "Register for VAT")
    }
  }

  "createManageYourTaxes" should {

    "throw exception if the profile cannot be retrieved from ggw" in new OtherServicesFactoryForTest {
      val regimes = RegimeRoots(sa = saRoot, ct = ctRoot, epaye = epayeRoot)
      implicit val user = User("userId", UserAuthority("userId", Regimes()), regimes, decryptedToken = None)
      implicit val request = FakeRequest()

      when(mockGovernmentGatewayConnector.profile("userId")).thenReturn(Future.successful(None))

      val result = factoryUnderTest.createManageYourTaxes(mockPortalUrlBuilder.buildPortalUrl)

      result.failed.futureValue shouldBe a [RuntimeException]
      result.failed.futureValue.getMessage shouldBe "Could not retrieve user profile from Government Gateway service"

     verifyZeroInteractions(mockAffinityGroupParser)
    }

    "throw exception if the affinity group cannot be parsed" in new OtherServicesFactoryForTest {
      val regimes = RegimeRoots(sa = saRoot, ct = ctRoot, epaye = epayeRoot)
      implicit val user = User("userId", UserAuthority("userId", Regimes()), regimes, decryptedToken = None)
      implicit val request = FakeRequest()

      val profile = Some(ProfileResponse(
        affinityGroup = AffinityGroup(AGENT),
        activeEnrolments = Set.empty)
      )

      when(mockGovernmentGatewayConnector.profile("userId")).thenReturn(Future.successful(profile))
      when(mockAffinityGroupParser.parseAffinityGroup).thenThrow(new RuntimeException("Affinity Group not found"))

      val result = factoryUnderTest.createManageYourTaxes(mockPortalUrlBuilder.buildPortalUrl)
      result.failed.futureValue shouldBe a [RuntimeException]
      result.failed.futureValue.getMessage shouldBe  "Affinity Group not found"
    }
  }


  private def saRoot = Some(SaRoot(SaUtr("sa-utr"), Map.empty[String, String]))

  private def ctRoot = Some(CtRoot(CtUtr("ct-utr"), Map.empty[String, String]))

  private def epayeRoot = Some(EpayeRoot(EmpRef("emp/ref"), EpayeLinks(None)))

  private def vatRoot = Some(VatRoot(Vrn("vrn"), Map.empty[String, String]))
}

abstract class OtherServicesFactoryForTest
  extends WithApplication(FakeApplication(additionalConfiguration = Map(
    "govuk-tax.Test.externalLinks.businessTax.manageTaxes.servicesHome" -> "https://secure.hmce.gov.uk/ecom/login/index.html",
    "govuk-tax.Test.externalLinks.businessTax.manageTaxes.ncts" -> "https://customs.hmrc.gov.uk/nctsPortalWebApp/ncts.portal?_nfpb=true&pageLabel=httpssIPageOnlineServicesAppNCTS_Home",
    "govuk-tax.Test.externalLinks.businessTax.manageTaxes.rcsl" -> "https://customs.hmrc.gov.uk/rcsl/httpssl/Home.do",
    "govuk-tax.Test.externalLinks.businessTax.manageTaxes.ecsl" -> "https://customs.hmrc.gov.uk/ecsl/httpssl/start.do",
    "govuk-tax.Test.externalLinks.businessTax.registration.otherWays" -> "http://www.hmrc.gov.uk/online/new.htm#2")))
  with PortalUrlBuilderMock
  with AffinityGroupParserMock
  with Matchers {

  val mockGovernmentGatewayConnector = mock[GovernmentGatewayConnector]

  val linkToHmrcWebsite = "http://www.hmrc.gov.uk/online/new.htm#2"
  val linkToHmrcOnlineRegistration = "http://businessRegistrationLink"

  val hmrcWebsiteLinkText = "HMRC website"

  val factoryUnderTest = new OtherServicesFactory(mockGovernmentGatewayConnector) with PortalUrlBuilderMock with MockedAffinityGroupParser

  protected def assertCorrectBusinessTaxRegistration(expectedLink: String, linkMessage: String)(implicit user: User) {

    val expectedRegistrationLink = if (linkMessage != hmrcWebsiteLinkText)
      Some(RenderableLinkMessage(LinkMessage(href = expectedLink, text = linkMessage, newWindow = false, sso = true, id = Some("businessRegistrationHref"))))
    else
      None

    val expected = BusinessTaxesRegistration(
      expectedRegistrationLink,
      RenderableLinkMessage(LinkMessage(href = linkToHmrcWebsite, text = hmrcWebsiteLinkText, newWindow = true, sso = false, id = Some("otherWaysHref"))))

    val result = factoryUnderTest.createBusinessTaxesRegistration(mockPortalUrlBuilder.buildPortalUrl)(user)

    result shouldBe expected
  }
}

