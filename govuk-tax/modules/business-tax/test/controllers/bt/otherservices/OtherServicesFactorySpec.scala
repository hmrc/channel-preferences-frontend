package controllers.bt.otherservices

import uk.gov.hmrc.common.BaseSpec
import views.helpers.{RenderableLinkMessage, LinkMessage}
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.common.microservice.domain.{RegimeRoots, User}
import uk.gov.hmrc.domain.{CtUtr, Vrn, EmpRef, SaUtr}
import uk.gov.hmrc.common.microservice.auth.domain.{Regimes, UserAuthority}
import org.scalatest.Matchers
import uk.gov.hmrc.common.microservice.governmentgateway.{GovernmentGatewayConnector, Enrolment, AffinityGroup, ProfileResponse}
import org.mockito.Mockito._
import uk.gov.hmrc.common.microservice.governmentgateway.AffinityGroupValue._
import play.api.test.{FakeRequest, WithApplication, FakeApplication}
import controllers.bt.testframework.mocks.{AffinityGroupParserMock, PortalUrlBuilderMock}
import org.mockito.stubbing.Answer
import org.mockito.invocation.InvocationOnMock
import uk.gov.hmrc.common.microservice.sa.domain.SaRoot
import uk.gov.hmrc.common.microservice.ct.domain.CtRoot
import uk.gov.hmrc.common.microservice.vat.domain.VatRoot
import uk.gov.hmrc.common.microservice.epaye.domain.{EpayeLinks, EpayeRoot}

class OtherServicesFactorySpec extends BaseSpec with MockitoSugar {

  "createOnlineServicesEnrolment " should {

    "return an OnlineServicesEnrolment object with the SSO link to access the portal" in new OtherServicesFactoryForTest {

      when(mockPortalUrlBuilder.buildPortalUrl("otherServicesEnrolment")).thenReturn("http://someLink")
      val expected = OnlineServicesEnrolment(RenderableLinkMessage(LinkMessage(href = "http://someLink", text = "here", sso = true, id=Some("otherServicesEnrolmentHref"))))
      val result = factoryUnderTest.createOnlineServicesEnrolment(mockPortalUrlBuilder.buildPortalUrl)

      result shouldBe expected
    }
  }

  "createOnlineServicesDeEnrolment " should {

    "return an OnlineServicesDeEnrolment object with the SSO link to access the portal" in new OtherServicesFactoryForTest {

      when(mockPortalUrlBuilder.buildPortalUrl("servicesDeEnrolment")).thenReturn("http://someLink")
      val expected = OnlineServicesEnrolment(RenderableLinkMessage(LinkMessage(href = "http://someLink", text = "here", sso = true, id = Some("servicesDeEnrolmentHref"))))
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

    "return None when the user affinity group is agent" in new OtherServicesFactoryForTest {

      implicit val user = User("userId", UserAuthority("userId", Regimes()), RegimeRoots(), decryptedToken = None)
      implicit val request = FakeRequest()
      when(mockAffinityGroupParser.parseAffinityGroup).thenReturn(AGENT)

      val expectedResponse = Some(ProfileResponse(
        affinityGroup = AffinityGroup(AGENT),
        activeEnrolments = Set.empty))

      when(mockGovernmentGatewayConnector.profile("userId")).thenReturn(expectedResponse)

      val result = factoryUnderTest.createManageYourTaxes(mockPortalUrlBuilder.buildPortalUrl)

      verify(mockGovernmentGatewayConnector).profile("userId")

      result shouldBe None
    }

    "throw a RuntimeException when the user profile cannot be retrieved from government gateway" in new OtherServicesFactoryForTest {

      implicit val user = User("userId", UserAuthority("userId", Regimes()), RegimeRoots(), decryptedToken = None)
      implicit val request = FakeRequest()

      when(mockGovernmentGatewayConnector.profile("userId")).thenReturn(None)
      when(mockAffinityGroupParser.parseAffinityGroup).thenReturn(INDIVIDUAL)

      intercept[RuntimeException] {
        factoryUnderTest.createManageYourTaxes(mockPortalUrlBuilder.buildPortalUrl)
      }

      verify(mockGovernmentGatewayConnector).profile("userId")

    }

    "return a list with links ordered by key for the user enrolments when the affinity group is individual" in new OtherServicesFactoryForTest {

      val regimes = RegimeRoots(sa = saRoot, ct = ctRoot, vat = vatRoot)

      implicit val user = User("userId", UserAuthority("userId", Regimes()), regimes, decryptedToken = None)
      implicit val request = FakeRequest()

      when(mockAffinityGroupParser.parseAffinityGroup).thenReturn(INDIVIDUAL)
      when(mockPortalUrlBuilder.buildPortalUrl(org.mockito.Matchers.any[String])).thenAnswer(new Answer[String] {
        def answer(invocation: InvocationOnMock): String = {
          "http://" + invocation.getArguments.toSeq.head
        }
      })

      val expectedResponse = Some(ProfileResponse(
        affinityGroup = AffinityGroup(INDIVIDUAL),
        activeEnrolments = Set(
          Enrolment("HMCE-ECSL-ORG"),
          Enrolment("HMCE-VATRSL-ORG"),
          Enrolment("HMRC-EU-REF-ORG")
        )))

      val postLinkText = Some("otherservices.manageTaxes.postLink.additionalLoginRequired")
      val expectedResult = Some(
        ManageYourTaxes(
          Seq(
            /* hmceecslorg */ RenderableLinkMessage(LinkMessage("https://customs.hmrc.gov.uk/ecsl/httpssl/start.do", "otherservices.manageTaxes.link.hmceecslorg", Some("hmceecslorgHref"), true, postLinkText, false)),
            /* hmcevatrslorg */ RenderableLinkMessage(LinkMessage("https://customs.hmrc.gov.uk/rcsl/httpssl/Home.do", "otherservices.manageTaxes.link.hmcevatrslorg", Some("hmcevatrslorgHref"), true, postLinkText, false)),
            /* hmrceureforg */ RenderableLinkMessage(LinkMessage("http://manageTaxes.euvat", "otherservices.manageTaxes.link.hmrceureforg", Some("hmrceureforgHref"), false, None, true))
          )
        )
      )

      when(mockGovernmentGatewayConnector.profile("userId")).thenReturn(expectedResponse)

      val result = factoryUnderTest.createManageYourTaxes(mockPortalUrlBuilder.buildPortalUrl)

      verify(mockGovernmentGatewayConnector).profile("userId")

      result shouldBe expectedResult
    }

    "return a list with links ordered by key for the user enrolments when the affinity group is organisation" in new OtherServicesFactoryForTest {

      val regimes = RegimeRoots(sa = saRoot, ct = ctRoot, vat = vatRoot)

      implicit val user = User("userId", UserAuthority("userId", Regimes()), regimes, decryptedToken = None)
      implicit val request = FakeRequest()

      when(mockAffinityGroupParser.parseAffinityGroup).thenReturn(ORGANISATION)
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
          Enrolment("HMCE-VATRSL-ORG"),
          Enrolment("HMRC-EMCS-ORG"),
          Enrolment("HMRC-ICS-ORG"),
          Enrolment("HMRC-MGD-ORG"),
          Enrolment("HMCE-NCTS-ORG"),
          Enrolment("HMCE-NES"),
          Enrolment("HMRC-NOVA-ORG"),
          Enrolment("HMCE-RO"),
          Enrolment("HMRC-ECW-IND"),
          Enrolment("HMCE-TO"),
          Enrolment("HMCE-ECSL-ORG"),
          Enrolment("HMRC-EU-REF-ORG")
        )))

      val postLinkText = Some("otherservices.manageTaxes.postLink.additionalLoginRequired")
      val expectedResult = Some(
        ManageYourTaxes(
          Seq(
            /* hmceddes */ RenderableLinkMessage(LinkMessage("https://secure.hmce.gov.uk/ecom/login/index.html", "otherservices.manageTaxes.link.hmceddes", Some("hmceddesHref"), true, postLinkText, false)),
            /* hmceebtiorg */ RenderableLinkMessage(LinkMessage("https://secure.hmce.gov.uk/ecom/login/index.html", "otherservices.manageTaxes.link.hmceebtiorg", Some("hmceebtiorgHref"), true, postLinkText, false)),
            /* hmceecslorg */ RenderableLinkMessage(LinkMessage("https://customs.hmrc.gov.uk/ecsl/httpssl/start.do", "otherservices.manageTaxes.link.hmceecslorg", Some("hmceecslorgHref"), true, postLinkText, false)),
            /* hmcenctsorg */ RenderableLinkMessage(LinkMessage("https://customs.hmrc.gov.uk/nctsPortalWebApp/ncts.portal?_nfpb=true&pageLabel=httpssIPageOnlineServicesAppNCTS_Home", "otherservices.manageTaxes.link.hmcenctsorg", Some("hmcenctsorgHref"), true, postLinkText, false)),
            /* hmcenes */ RenderableLinkMessage(LinkMessage("https://secure.hmce.gov.uk/ecom/login/index.html", "otherservices.manageTaxes.link.hmcenes", Some("hmcenesHref"), true, postLinkText, false)),
            /* hmcero1 */ RenderableLinkMessage(LinkMessage("https://secure.hmce.gov.uk/ecom/login/index.html", "otherservices.manageTaxes.link.hmcero1", Some("hmcero1Href"), true, postLinkText, false)),
            /* hmcero2 */ RenderableLinkMessage(LinkMessage("https://secure.hmce.gov.uk/ecom/login/index.html", "otherservices.manageTaxes.link.hmcero2", Some("hmcero2Href"), true, postLinkText, false)),
            /* hmcero3 */ RenderableLinkMessage(LinkMessage("https://secure.hmce.gov.uk/ecom/login/index.html", "otherservices.manageTaxes.link.hmcero3", Some("hmcero3Href"), true, postLinkText, false)),
            /* hmceto */ RenderableLinkMessage(LinkMessage("https://secure.hmce.gov.uk/ecom/login/index.html", "otherservices.manageTaxes.link.hmceto", Some("hmcetoHref"), true, postLinkText, false)),
            /* hmcevatrslorg */ RenderableLinkMessage(LinkMessage("https://customs.hmrc.gov.uk/rcsl/httpssl/Home.do", "otherservices.manageTaxes.link.hmcevatrslorg", Some("hmcevatrslorgHref"), true, postLinkText, false)),
            /* hmrcemcsorg */ RenderableLinkMessage(LinkMessage("http://manageTaxes.emcs", "otherservices.manageTaxes.link.hmrcemcsorg", Some("hmrcemcsorgHref"), false, None, true)),
            /* hmrceureforg */ RenderableLinkMessage(LinkMessage("http://manageTaxes.euvat", "otherservices.manageTaxes.link.hmrceureforg", Some("hmrceureforgHref"), false, None, true)),
            /* hmrcicsorg */ RenderableLinkMessage(LinkMessage("http://manageTaxes.ics", "otherservices.manageTaxes.link.hmrcicsorg", Some("hmrcicsorgHref"), false, None, true)),
            /* hmrcmgdorg */ RenderableLinkMessage(LinkMessage("http://manageTaxes.machinegames", "otherservices.manageTaxes.link.hmrcmgdorg", Some("hmrcmgdorgHref"), false, None, true))
          )
        )
      )

      when(mockGovernmentGatewayConnector.profile("userId")).thenReturn(expectedResponse)

      val result = factoryUnderTest.createManageYourTaxes(mockPortalUrlBuilder.buildPortalUrl)

      verify(mockGovernmentGatewayConnector).profile("userId")

      result shouldBe expectedResult
    }

    "throw an exception if the affinity group is not found" in new OtherServicesFactoryForTest {

      implicit val user = User("userId", UserAuthority("userId", Regimes()), RegimeRoots(), decryptedToken = None)
      implicit val request = FakeRequest()

      val expectedResponse = Some(ProfileResponse(
        affinityGroup = AffinityGroup(INDIVIDUAL),
        activeEnrolments = Set.empty))
      when(mockGovernmentGatewayConnector.profile("userId")).thenReturn(expectedResponse)
      when(mockAffinityGroupParser.parseAffinityGroup).thenThrow(new InternalError)

      intercept[InternalError] {
        factoryUnderTest.createManageYourTaxes(mockPortalUrlBuilder.buildPortalUrl)
      }
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

