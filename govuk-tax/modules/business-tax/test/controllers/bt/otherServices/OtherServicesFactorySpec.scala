package controllers.bt.otherservices

import uk.gov.hmrc.common.BaseSpec
import views.helpers.{RenderableLinkMessage, LinkMessage}
import org.scalatest.mock.MockitoSugar
import play.api.mvc.Request
import uk.gov.hmrc.common.microservice.domain.{RegimeRoots, User}
import uk.gov.hmrc.common.microservice.sa.domain.SaDomain.SaRoot
import uk.gov.hmrc.domain.{CtUtr, Vrn, EmpRef, SaUtr}
import uk.gov.hmrc.common.microservice.epaye.domain.EpayeDomain.{EpayeLinks, EpayeRoot}
import uk.gov.hmrc.common.microservice.vat.domain.VatDomain.VatRoot
import uk.gov.hmrc.common.microservice.ct.domain.CtDomain.CtRoot
import uk.gov.hmrc.common.microservice.auth.domain.{Regimes, UserAuthority}
import org.scalatest.{Matchers, TestData}
import uk.gov.hmrc.common.microservice.governmentgateway.{Enrolment, AffinityGroup, ProfileResponse, GovernmentGatewayMicroService}
import org.mockito.Mockito._
import uk.gov.hmrc.common.microservice.governmentgateway.AffinityGroupValue._
import play.api.i18n.Messages
import play.api.test.{WithApplication, FakeApplication}
import controllers.bt.testframework.mocks.{ConnectorMocks, PortalUrlBuilderMock}
import org.mockito.stubbing.Answer
import org.mockito.invocation.InvocationOnMock

class OtherServicesFactorySpec extends BaseSpec with MockitoSugar {

  "createOnlineServicesEnrolment " should {

    "return an OnlineServicesEnrolment object with the SSO link to access the portal" in new OtherServicesFactoryForTest {

      when(mockPortalUrlBuilder.buildPortalUrl("otherServicesEnrolment")).thenReturn("http://someLink")
      val expected = OnlineServicesEnrolment(RenderableLinkMessage(LinkMessage("http://someLink", "here")))
      val result = factoryUnderTest.createOnlineServicesEnrolment(mockPortalUrlBuilder.buildPortalUrl)

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

      assertCorrectBusinessTaxRegistration(linkToHmrcOnlineRegistration, "Register for SA, CT, employers PAYE, or VAT")
    }

    "return a BusinessTaxRegistration object containing registration link for sa, ct and epaye regimes if only vat is defined" in new OtherServicesFactoryForTest {

      val regimes = RegimeRoots(vat = vatRoot)
      
      implicit val user = User("userId", UserAuthority("userId", Regimes()), regimes, decryptedToken = None)

      assertCorrectBusinessTaxRegistration(linkToHmrcOnlineRegistration, "Register for SA, CT, or employers PAYE")
    }

    "return a BusinessTaxRegistration object containing registration link for sa, ct and vat regimes if only epaye is defined" in new OtherServicesFactoryForTest {

      val regimes = RegimeRoots(epaye = epayeRoot)
      
      implicit val user = User("userId", UserAuthority("userId", Regimes()), regimes, decryptedToken = None)

      assertCorrectBusinessTaxRegistration(linkToHmrcOnlineRegistration, "Register for SA, CT, or VAT")
    }

    "return a BusinessTaxRegistration object containing registration link for sa and ct regimes if epaye and vat are defined" in new OtherServicesFactoryForTest {

      val regimes = RegimeRoots(epaye = epayeRoot, vat = vatRoot)
      
      implicit val user = User("userId", UserAuthority("userId", Regimes()), regimes, decryptedToken = None)

      assertCorrectBusinessTaxRegistration(linkToHmrcOnlineRegistration, "Register for SA, or CT")
    }

    "return a BusinessTaxRegistration object containing registration link for sa, epaye and vat if only ct is defined" in new OtherServicesFactoryForTest {

      val regimes = RegimeRoots(ct = ctRoot)
      
      implicit val user = User("userId", UserAuthority("userId", Regimes()), regimes, decryptedToken = None)

      assertCorrectBusinessTaxRegistration(linkToHmrcOnlineRegistration, "Register for SA, employers PAYE, or VAT")
    }

    "return a BusinessTaxRegistration object containing registration link for sa and epaye if ct and vat are defined" in new OtherServicesFactoryForTest {

      val regimes = RegimeRoots(ct = ctRoot, vat = vatRoot)
      
      implicit val user = User("userId", UserAuthority("userId", Regimes()), regimes, decryptedToken = None)

      assertCorrectBusinessTaxRegistration(linkToHmrcOnlineRegistration, "Register for SA, or employers PAYE")
    }

    "return a BusinessTaxRegistration object containing registration link for sa and vat if ct and epaye are defined" in new OtherServicesFactoryForTest {

      val regimes = RegimeRoots(ct = ctRoot, epaye = epayeRoot)
      
      implicit val user = User("userId", UserAuthority("userId", Regimes()), regimes, decryptedToken = None)

      assertCorrectBusinessTaxRegistration(linkToHmrcOnlineRegistration, "Register for SA, or VAT")
    }

    "return a BusinessTaxRegistration object containing registration link for ct, epaye and vat if only sa is defined" in new OtherServicesFactoryForTest {

      val regimes = RegimeRoots(sa = saRoot)
      
      implicit val user = User("userId", UserAuthority("userId", Regimes()), regimes, decryptedToken = None)

      assertCorrectBusinessTaxRegistration(linkToHmrcOnlineRegistration, "Register for CT, employers PAYE, or VAT")
    }

    "return a BusinessTaxRegistration object containing registration link for ct and epaye if sa and vat are defined" in new OtherServicesFactoryForTest {

      val regimes = RegimeRoots(sa = saRoot, vat = vatRoot)
      
      implicit val user = User("userId", UserAuthority("userId", Regimes()), regimes, decryptedToken = None)

      assertCorrectBusinessTaxRegistration(linkToHmrcOnlineRegistration, "Register for CT, or employers PAYE")
    }

    "return a BusinessTaxRegistration object containing registration link for ct and vat if sa and epaye are defined" in new OtherServicesFactoryForTest {

      val regimes = RegimeRoots(sa = saRoot, epaye = epayeRoot)
      
      implicit val user = User("userId", UserAuthority("userId", Regimes()), regimes, decryptedToken = None)

      assertCorrectBusinessTaxRegistration(linkToHmrcOnlineRegistration, "Register for CT, or VAT")
    }

    "return a BusinessTaxRegistration object containing registration link for employers paye and vat if sa and ct are defined" in new OtherServicesFactoryForTest {

      val regimes = RegimeRoots(sa = saRoot, ct = ctRoot)
      
      implicit val user = User("userId", UserAuthority("userId", Regimes()), regimes, decryptedToken = None)

      assertCorrectBusinessTaxRegistration(linkToHmrcOnlineRegistration, "Register for employers PAYE, or VAT")
    }

    "return a BusinessTaxRegistration object containing registration link for employers paye  if sa, ct and vat are defined" in new OtherServicesFactoryForTest {

      val regimes = RegimeRoots(sa = saRoot, ct = ctRoot, vat = vatRoot)
      
      implicit val user = User("userId", UserAuthority("userId", Regimes()), regimes, decryptedToken = None)

      assertCorrectBusinessTaxRegistration(linkToHmrcOnlineRegistration, "Register for employers PAYE")
    }

    "return a BusinessTaxRegistration object containing registration link for vat  if sa, ct and epaye are defined" in new OtherServicesFactoryForTest {

      val regimes = RegimeRoots(sa = saRoot, ct = ctRoot, epaye = epayeRoot)
      implicit val user = User("userId", UserAuthority("userId", Regimes()), regimes, decryptedToken = None)

      assertCorrectBusinessTaxRegistration(linkToHmrcOnlineRegistration, "Register for VAT")
    }
  }

  "createManageYourTaxes" should {

    "return None when the user affinity group is agent" in new OtherServicesFactoryForTest {

      implicit val user = User("userId", UserAuthority("userId", Regimes()), RegimeRoots(), decryptedToken = None)

      val expectedResponse = Some(ProfileResponse(
        affinityGroup = AffinityGroup(AGENT),
        activeEnrolments = Set.empty))

      when(mockGovernmentGatewayMicroService.profile("userId")).thenReturn(expectedResponse)

      val result = factoryUnderTest.createManageYourTaxes(mockPortalUrlBuilder.buildPortalUrl)

      verify(mockGovernmentGatewayMicroService).profile("userId")

      result shouldBe None
    }

    "throw a RuntimeException when the user profile cannot be retrieved from government gateway" in new OtherServicesFactoryForTest {

      implicit val user = User("userId", UserAuthority("userId", Regimes()), RegimeRoots(), decryptedToken = None)

      when(mockGovernmentGatewayMicroService.profile("userId")).thenReturn(None)

      intercept[RuntimeException] {
        factoryUnderTest.createManageYourTaxes(mockPortalUrlBuilder.buildPortalUrl)
      }

      verify(mockGovernmentGatewayMicroService).profile("userId")

    }

    "return a list with links ordered by key for the user enrolments when the affinity group is individual" in new OtherServicesFactoryForTest {

      val regimes = RegimeRoots(sa = saRoot, ct = ctRoot, vat = vatRoot)

      implicit val user = User("userId", UserAuthority("userId", Regimes()), regimes, decryptedToken = None)

      when(mockPortalUrlBuilder.buildPortalUrl(org.mockito.Matchers.any[String])).thenAnswer(new Answer[String] {
        def answer(invocation: InvocationOnMock): String = {
          "http://" + invocation.getArguments.toSeq.head
        }
      })

      val expectedResponse = Some(ProfileResponse(
        affinityGroup = AffinityGroup(INDIVIDUAL),
        activeEnrolments = Set(
          Enrolment("HMCE-ECSL-ORG"),
          Enrolment("HMRC-EU-REF-ORG"),
          Enrolment("HMRC-VATRSL-ORG")
        )))

      val postLinkText = Some("otherservices.manageTaxes.postLink.additionalLoginRequired")
      val expectedResult = Some(
        ManageYourTaxes(
          Seq(
            /* hmceecslorg */ RenderableLinkMessage(LinkMessage("https://customs.hmrc.gov.uk/ecsl/httpssl/start.do", "otherservices.manageTaxes.link.hmceecslorg", None, true, postLinkText)),
            /* hmrceureforg */ RenderableLinkMessage(LinkMessage("http://destinationPath.manageTaxes.euvat", "otherservices.manageTaxes.link.hmrceureforg", None, false, None, true)),
            /* hmrcvatrslorg */ RenderableLinkMessage(LinkMessage("https://customs.hmrc.gov.uk/rcsl/httpssl/Home.do", "otherservices.manageTaxes.link.hmrcvatrslorg", None, true, postLinkText))
          )
        )
      )

      when(mockGovernmentGatewayMicroService.profile("userId")).thenReturn(expectedResponse)

      val result = factoryUnderTest.createManageYourTaxes(mockPortalUrlBuilder.buildPortalUrl)

      verify(mockGovernmentGatewayMicroService).profile("userId")

      result shouldBe expectedResult
    }


    "return a list with links ordered by key for the user enrolments when the affinity group is organisation" in new OtherServicesFactoryForTest {

      val regimes = RegimeRoots(sa = saRoot, ct = ctRoot, vat = vatRoot)

      implicit val user = User("userId", UserAuthority("userId", Regimes()), regimes, decryptedToken = None)

      when(mockPortalUrlBuilder.buildPortalUrl(org.mockito.Matchers.any[String])).thenAnswer(new Answer[String] {
        def answer(invocation: InvocationOnMock): String = {
          "http://" + invocation.getArguments.toSeq.head
        }
      })

      val expectedResponse = Some(ProfileResponse(
        affinityGroup = AffinityGroup(ORGANISATION),
        activeEnrolments = Set(
          Enrolment("HMCE-DDES"),
          Enrolment("HMCE-EBTI-ORG"),
          Enrolment("HMRC-EMCS-ORG"),
          Enrolment("HMRC-ICS-ORG"),
          Enrolment("HMRC-MGD-ORG"),
          Enrolment("HMCE-NCTS-ORG"),
          Enrolment("HMCE-NES"),
          Enrolment("HMRC-NOVA-ORG"),
          Enrolment("HMCE-RO"),
          Enrolment("HMRC-ECW-IND"),
          Enrolment("HMCE-TO")
        )))

      val postLinkText = Some("otherservices.manageTaxes.postLink.additionalLoginRequired")
      val expectedResult = Some(
        ManageYourTaxes(
          Seq(
            /* hmceddes */ RenderableLinkMessage(LinkMessage("https://secure.hmce.gov.uk/ecom/login/index.html", "otherservices.manageTaxes.link.hmceddes", None, true, postLinkText)),
            /* hmceebtiorg */ RenderableLinkMessage(LinkMessage("https://secure.hmce.gov.uk/ecom/login/index.html", "otherservices.manageTaxes.link.hmceebtiorg", None, true, postLinkText)),
            /* hmcenctsorg */ RenderableLinkMessage(LinkMessage("https://customs.hmrc.gov.uk/nctsPortalWebApp/ncts.portal?_nfpb=true&pageLabel=httpssIPageOnlineServicesAppNCTS_Home", "otherservices.manageTaxes.link.hmcenctsorg", None, true, postLinkText)),
            /* hmcenes */ RenderableLinkMessage(LinkMessage("https://secure.hmce.gov.uk/ecom/login/index.html", "otherservices.manageTaxes.link.hmcenes", None, true, postLinkText)),
            /* hmcero1 */ RenderableLinkMessage(LinkMessage("https://secure.hmce.gov.uk/ecom/login/index.html", "otherservices.manageTaxes.link.hmcero1", None, true, postLinkText)),
            /* hmcero2 */ RenderableLinkMessage(LinkMessage("https://secure.hmce.gov.uk/ecom/login/index.html", "otherservices.manageTaxes.link.hmcero2", None, true, postLinkText)),
            /* hmcero3 */ RenderableLinkMessage(LinkMessage("https://secure.hmce.gov.uk/ecom/login/index.html", "otherservices.manageTaxes.link.hmcero3", None, true, postLinkText)),
            /* hmceto */ RenderableLinkMessage(LinkMessage("https://secure.hmce.gov.uk/ecom/login/index.html", "otherservices.manageTaxes.link.hmceto", None, true, postLinkText)),
            /* hmrcemcsorg */ RenderableLinkMessage(LinkMessage("http://destinationPath.manageTaxes.emcs", "otherservices.manageTaxes.link.hmrcemcsorg", None, false, None, true)),
            /* hmrcicsorg */ RenderableLinkMessage(LinkMessage("http://destinationPath.manageTaxes.ics", "otherservices.manageTaxes.link.hmrcicsorg", None, false, None, true)),
            /* hmrcmgdorg */ RenderableLinkMessage(LinkMessage("http://destinationPath.manageTaxes.machinegames", "otherservices.manageTaxes.link.hmrcmgdorg", None, false, None, true)),
            /* hmrcnovaorg */ RenderableLinkMessage(LinkMessage("http://destinationPath.manageTaxes.nova", "otherservices.manageTaxes.link.hmrcnovaorg", None, false, None, true))
          )
        )
      )

      when(mockGovernmentGatewayMicroService.profile("userId")).thenReturn(expectedResponse)

      val result = factoryUnderTest.createManageYourTaxes(mockPortalUrlBuilder.buildPortalUrl)

      verify(mockGovernmentGatewayMicroService).profile("userId")

      result shouldBe expectedResult
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
    "govuk-tax.Test.externalLinks.businessTax.manageTaxes.ecsl" -> "https://customs.hmrc.gov.uk/ecsl/httpssl/start.do")))
  with PortalUrlBuilderMock
  with ConnectorMocks 
  with Matchers {

  val linkToHmrcWebsite = "http://www.hmrc.gov.uk/online/new.htm#2"
  val linkToHmrcOnlineRegistration = "https://online.hmrc.gov.uk/registration/newbusiness/business-allowed"
  val hmrcWebsiteLinkText = "HMRC website"

  val factoryUnderTest = new OtherServicesFactory(mockGovernmentGatewayMicroService)

  def assertCorrectBusinessTaxRegistration(expectedLink: String, linkMessage: String)(implicit user: User) {
    def linkObj = Some(RenderableLinkMessage(LinkMessage(expectedLink, linkMessage)))
    val link = if (linkMessage != hmrcWebsiteLinkText) linkObj else None
    val expected = BusinessTaxesRegistration(
      link,
      RenderableLinkMessage(LinkMessage(linkToHmrcWebsite, hmrcWebsiteLinkText)))
    val result = factoryUnderTest.createBusinessTaxesRegistration(mockPortalUrlBuilder.buildPortalUrl)(user)

    result shouldBe expected
  }
}

