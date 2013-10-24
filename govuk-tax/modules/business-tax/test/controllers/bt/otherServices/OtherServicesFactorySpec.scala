package controllers.bt.otherservices

import uk.gov.hmrc.common.BaseSpec
import views.helpers.{LinkMessage, RenderableLinkMessage}
import org.scalatest.mock.MockitoSugar
import play.api.mvc.Request
import uk.gov.hmrc.common.microservice.domain.{RegimeRoots, User}
import uk.gov.hmrc.common.microservice.sa.domain.SaDomain.SaRoot
import uk.gov.hmrc.domain.{CtUtr, Vrn, EmpRef, SaUtr}
import uk.gov.hmrc.common.microservice.epaye.domain.EpayeDomain.{EpayeLinks, EpayeRoot}
import uk.gov.hmrc.common.microservice.vat.domain.VatDomain.VatRoot
import uk.gov.hmrc.common.microservice.ct.domain.CtDomain.CtRoot
import uk.gov.hmrc.common.microservice.auth.domain.{Regimes, UserAuthority}
import org.scalatest.TestData
import uk.gov.hmrc.common.microservice.governmentgateway.{Enrolment, AffinityGroup, ProfileResponse, GovernmentGatewayMicroService}
import org.mockito.Mockito._
import uk.gov.hmrc.common.microservice.governmentgateway.AffinityGroupValue._
import play.api.i18n.Messages


class OtherServicesFactorySpec extends BaseSpec with MockitoSugar {

  val ggMicroServiceMock = mock[GovernmentGatewayMicroService]

  val factory = new OtherServicesFactory(ggMicroServiceMock) {
    override def buildPortalUrl(destinationPathKey: String)(implicit request: Request[AnyRef], user: User): String = destinationPathKey
  }

  override protected def beforeEach(testData: TestData): Unit = {
    super.beforeEach(testData)
    reset(ggMicroServiceMock)
  }

  val linkToHmrcWebsite = "http://www.hmrc.gov.uk/online/new.htm#2"
  val linkToHmrcOnlineRegistration = "https://online.hmrc.gov.uk/registration/newbusiness/business-allowed"
  val hmrcWebsiteLinkText = "HMRC website"

  "createOnlineServicesEnrolment " should {

    "return an OnlineServicesEnrolment object with the SSO link to access the portal" in {

      implicit val mockRequest = mock[Request[AnyRef]]
      implicit val mockUser = mock[User]

      val expected = OnlineServicesEnrolment(RenderableLinkMessage(LinkMessage("otherServicesEnrolment", "here")))
      val result = factory.createOnlineServicesEnrolment

      result shouldBe expected
    }
  }

  "createBusinessTaxesRegistration" should {
    "return a BusinessTaxRegistration object containing only the link to the hmrc website if epaye, vat and sa are defined" in {

      val regimes = RegimeRoots(sa = saRoot, vat = vatRoot, epaye = epayeRoot)
      implicit val mockRequest = mock[Request[AnyRef]]
      implicit val user = User("userId", UserAuthority("userId", Regimes()), regimes, decryptedToken = None)

      assertCorrectBusinessTaxRegistration(linkToHmrcWebsite, hmrcWebsiteLinkText)
    }

    "return a BusinessTaxRegistration object containing only the link to the hmrc website if epaye, vat and ct are defined" in {

      val regimes = RegimeRoots(ct = ctRoot, vat = vatRoot, epaye = epayeRoot)
      implicit val mockRequest = mock[Request[AnyRef]]
      implicit val user = User("userId", UserAuthority("userId", Regimes()), regimes, decryptedToken = None)

      assertCorrectBusinessTaxRegistration(linkToHmrcWebsite, hmrcWebsiteLinkText)
    }

    "return a BusinessTaxRegistration object containing only the link to the hmrc website if epaye, vat, sa and ct are defined" in {

      val regimes = RegimeRoots(sa = saRoot, ct = ctRoot, vat = vatRoot, epaye = epayeRoot)
      implicit val mockRequest = mock[Request[AnyRef]]
      implicit val user = User("userId", UserAuthority("userId", Regimes()), regimes, decryptedToken = None)

      assertCorrectBusinessTaxRegistration(linkToHmrcWebsite, hmrcWebsiteLinkText)
    }

    "return a BusinessTaxRegistration object containing registration link for all the regimes if none of the regimes are defined" in {

      val regimes = RegimeRoots()
      implicit val mockRequest = mock[Request[AnyRef]]
      implicit val user = User("userId", UserAuthority("userId", Regimes()), regimes, decryptedToken = None)

      assertCorrectBusinessTaxRegistration(linkToHmrcOnlineRegistration, "Register for SA, CT, employers PAYE, or VAT")
    }

    "return a BusinessTaxRegistration object containing registration link for sa, ct and epaye regimes if only vat is defined" in {

      val regimes = RegimeRoots(vat = vatRoot)
      implicit val mockRequest = mock[Request[AnyRef]]
      implicit val user = User("userId", UserAuthority("userId", Regimes()), regimes, decryptedToken = None)

      assertCorrectBusinessTaxRegistration(linkToHmrcOnlineRegistration, "Register for SA, CT, or employers PAYE")
    }

    "return a BusinessTaxRegistration object containing registration link for sa, ct and vat regimes if only epaye is defined" in {

      val regimes = RegimeRoots(epaye = epayeRoot)
      implicit val mockRequest = mock[Request[AnyRef]]
      implicit val user = User("userId", UserAuthority("userId", Regimes()), regimes, decryptedToken = None)

      assertCorrectBusinessTaxRegistration(linkToHmrcOnlineRegistration, "Register for SA, CT, or VAT")
    }

    "return a BusinessTaxRegistration object containing registration link for sa and ct regimes if epaye and vat are defined" in {

      val regimes = RegimeRoots(epaye = epayeRoot, vat = vatRoot)
      implicit val mockRequest = mock[Request[AnyRef]]
      implicit val user = User("userId", UserAuthority("userId", Regimes()), regimes, decryptedToken = None)

      assertCorrectBusinessTaxRegistration(linkToHmrcOnlineRegistration, "Register for SA, or CT")
    }

    "return a BusinessTaxRegistration object containing registration link for sa, epaye and vat if only ct is defined" in {

      val regimes = RegimeRoots(ct = ctRoot)
      implicit val mockRequest = mock[Request[AnyRef]]
      implicit val user = User("userId", UserAuthority("userId", Regimes()), regimes, decryptedToken = None)

      assertCorrectBusinessTaxRegistration(linkToHmrcOnlineRegistration, "Register for SA, employers PAYE, or VAT")
    }

    "return a BusinessTaxRegistration object containing registration link for sa and epaye if ct and vat are defined" in {

      val regimes = RegimeRoots(ct = ctRoot, vat = vatRoot)
      implicit val mockRequest = mock[Request[AnyRef]]
      implicit val user = User("userId", UserAuthority("userId", Regimes()), regimes, decryptedToken = None)

      assertCorrectBusinessTaxRegistration(linkToHmrcOnlineRegistration, "Register for SA, or employers PAYE")
    }

    "return a BusinessTaxRegistration object containing registration link for sa and vat if ct and epaye are defined" in {

      val regimes = RegimeRoots(ct = ctRoot, epaye = epayeRoot)
      implicit val mockRequest = mock[Request[AnyRef]]
      implicit val user = User("userId", UserAuthority("userId", Regimes()), regimes, decryptedToken = None)

      assertCorrectBusinessTaxRegistration(linkToHmrcOnlineRegistration, "Register for SA, or VAT")
    }

    "return a BusinessTaxRegistration object containing registration link for ct, epaye and vat if only sa is defined" in {

      val regimes = RegimeRoots(sa = saRoot)
      implicit val mockRequest = mock[Request[AnyRef]]
      implicit val user = User("userId", UserAuthority("userId", Regimes()), regimes, decryptedToken = None)

      assertCorrectBusinessTaxRegistration(linkToHmrcOnlineRegistration, "Register for CT, employers PAYE, or VAT")
    }

    "return a BusinessTaxRegistration object containing registration link for ct and epaye if sa and vat are defined" in {

      val regimes = RegimeRoots(sa = saRoot, vat = vatRoot)
      implicit val mockRequest = mock[Request[AnyRef]]
      implicit val user = User("userId", UserAuthority("userId", Regimes()), regimes, decryptedToken = None)

      assertCorrectBusinessTaxRegistration(linkToHmrcOnlineRegistration, "Register for CT, or employers PAYE")
    }

    "return a BusinessTaxRegistration object containing registration link for ct and vat if sa and epaye are defined" in {

      val regimes = RegimeRoots(sa = saRoot, epaye = epayeRoot)
      implicit val mockRequest = mock[Request[AnyRef]]
      implicit val user = User("userId", UserAuthority("userId", Regimes()), regimes, decryptedToken = None)

      assertCorrectBusinessTaxRegistration(linkToHmrcOnlineRegistration, "Register for CT, or VAT")
    }

    "return a BusinessTaxRegistration object containing registration link for employers paye and vat if sa and ct are defined" in {

      val regimes = RegimeRoots(sa = saRoot, ct = ctRoot)
      implicit val mockRequest = mock[Request[AnyRef]]
      implicit val user = User("userId", UserAuthority("userId", Regimes()), regimes, decryptedToken = None)

      assertCorrectBusinessTaxRegistration(linkToHmrcOnlineRegistration, "Register for employers PAYE, or VAT")
    }

    "return a BusinessTaxRegistration object containing registration link for employers paye  if sa, ct and vat are defined" in {

      val regimes = RegimeRoots(sa = saRoot, ct = ctRoot, vat = vatRoot)
      implicit val mockRequest = mock[Request[AnyRef]]
      implicit val user = User("userId", UserAuthority("userId", Regimes()), regimes, decryptedToken = None)

      assertCorrectBusinessTaxRegistration(linkToHmrcOnlineRegistration, "Register for employers PAYE")
    }

    "return a BusinessTaxRegistration object containing registration link for vat  if sa, ct and epaye are defined" in {

      val regimes = RegimeRoots(sa = saRoot, ct = ctRoot, epaye = epayeRoot)
      implicit val mockRequest = mock[Request[AnyRef]]
      implicit val user = User("userId", UserAuthority("userId", Regimes()), regimes, decryptedToken = None)

      assertCorrectBusinessTaxRegistration(linkToHmrcOnlineRegistration, "Register for VAT")
    }
  }

  "createManageYourTaxes" should {

    "return None when the user affinity group is agent" in {

      implicit val mockRequest = mock[Request[AnyRef]]
      implicit val user = User("userId", UserAuthority("userId", Regimes()), RegimeRoots(), decryptedToken = None)

      val expectedResponse = Some(ProfileResponse(
        affinityGroup = AffinityGroup(AGENT),
        activeEnrolments = Set.empty))

      when(ggMicroServiceMock.profile("userId")).thenReturn(expectedResponse)

      val result = factory.createManageYourTaxes

      verify(ggMicroServiceMock).profile("userId")

      result shouldBe None
    }

    "throw a RuntimeException when the user profile cannot be retrieved from government gateway" in {

      implicit val mockRequest = mock[Request[AnyRef]]
      implicit val user = User("userId", UserAuthority("userId", Regimes()), RegimeRoots(), decryptedToken = None)

      when(ggMicroServiceMock.profile("userId")).thenReturn(None)

      intercept[RuntimeException] {
        factory.createManageYourTaxes
      }

      verify(ggMicroServiceMock).profile("userId")

    }

    "return a list with links ordered by key for the user enrolments when the affinity group is individual" in {

      implicit val mockRequest = mock[Request[AnyRef]]
      implicit val user = User("userId", UserAuthority("userId", Regimes()), RegimeRoots(), decryptedToken = None)

      val expectedResponse = Some(ProfileResponse(
        affinityGroup = AffinityGroup(INDIVIDUAL),
        activeEnrolments = Set(
          Enrolment("HMCE-DDES"),
          Enrolment("HMCE-EBTI-ORG"),
          Enrolment("HMCE-ECSL-ORG"),
          Enrolment("HMCE-NCTS-ORG"),
          Enrolment("HMCE-NES"),
          Enrolment("HMCE-RO"),
          Enrolment("HMCE-TO"),
          Enrolment("HMRC-ECW-IND"),
          Enrolment("HMRC-EMCS-ORG"),
          Enrolment("HMRC-EU-REF-ORG"),
          Enrolment("HMRC-ICS-ORG"),
          Enrolment("HMRC-MGD-ORG"),
          Enrolment("HMRC-NOVA-ORG"),
          Enrolment("HMRC-VATRSL-ORG")
        )))

      val expectedResult = Some(
        ManageYourTaxes(
          Seq(
            RenderableLinkMessage(LinkMessage("destinationPath.managedtaxes.servicesHome", Messages("otherservices.managetaxes.link.hmceddes"))),
            RenderableLinkMessage(LinkMessage("destinationPath.managedtaxes.servicesHome", Messages("otherservices.managetaxes.link.hmceebtiorg"))),
            RenderableLinkMessage(LinkMessage("destinationPath.managedtaxes.ecsl", Messages("otherservices.managetaxes.link.hmceecslorg"))),
            RenderableLinkMessage(LinkMessage("destinationPath.managedtaxes.ncts", Messages("otherservices.managetaxes.link.hmcenctsorg"))),
            RenderableLinkMessage(LinkMessage("destinationPath.managedtaxes.servicesHome", Messages("otherservices.managetaxes.link.hmcenes"))),
            RenderableLinkMessage(LinkMessage("destinationPath.managedtaxes.servicesHome", Messages("otherservices.managetaxes.link.hmcero1"))),
            RenderableLinkMessage(LinkMessage("destinationPath.managedtaxes.servicesHome", Messages("otherservices.managetaxes.link.hmcero2"))),
            RenderableLinkMessage(LinkMessage("destinationPath.managedtaxes.servicesHome", Messages("otherservices.managetaxes.link.hmcero3"))),
            RenderableLinkMessage(LinkMessage("destinationPath.managedtaxes.servicesHome", Messages("otherservices.managetaxes.link.hmceto"))),
            RenderableLinkMessage(LinkMessage("destinationPath.managedtaxes.er1", Messages("otherservices.managetaxes.link.hmrcecwind"))),
            RenderableLinkMessage(LinkMessage("destinationPath.managedtaxes.emcs", Messages("otherservices.managetaxes.link.hmrcemcsorg"))),
            RenderableLinkMessage(LinkMessage("destinationPath.managedtaxes.euvat", Messages("otherservices.managetaxes.link.hmrceureforg"))),
            RenderableLinkMessage(LinkMessage("destinationPath.managedtaxes.ics", Messages("otherservices.managetaxes.link.hmrcicsorg"))),
            RenderableLinkMessage(LinkMessage("destinationPath.managedtaxes.machinegames", Messages("otherservices.managetaxes.link.hmrcmgdorg"))),
            RenderableLinkMessage(LinkMessage("destinationPath.managedtaxes.nova", Messages("otherservices.managetaxes.link.hmrcnovaorg"))),
            RenderableLinkMessage(LinkMessage("destinationPath.managedtaxes.rcsl", Messages("otherservices.managetaxes.link.hmrcvatrslorg")))

          )
        )
      )

      when(ggMicroServiceMock.profile("userId")).thenReturn(expectedResponse)

      val result = factory.createManageYourTaxes

      verify(ggMicroServiceMock).profile("userId")

      result shouldBe expectedResult
    }

  }

  private def assertCorrectBusinessTaxRegistration(expectedLink: String, linkMessage: String)(implicit user: User) {
    def linkObj = Some(RenderableLinkMessage(LinkMessage(expectedLink, linkMessage)))
    val link = if (linkMessage != hmrcWebsiteLinkText) linkObj else None
    val expected = BusinessTaxesRegistration(
      link,
      RenderableLinkMessage(LinkMessage(linkToHmrcWebsite, hmrcWebsiteLinkText)))
    val result = factory.createBusinessTaxesRegistration(user)

    result shouldBe expected
  }

  private def saRoot = Some(SaRoot(SaUtr("sa-utr"), Map.empty[String, String]))

  private def ctRoot = Some(CtRoot(CtUtr("ct-utr"), Map.empty[String, String]))

  private def epayeRoot = Some(EpayeRoot(EmpRef("emp/ref"), EpayeLinks(None)))

  private def vatRoot = Some(VatRoot(Vrn("vrn"), Map.empty[String, String]))

}
