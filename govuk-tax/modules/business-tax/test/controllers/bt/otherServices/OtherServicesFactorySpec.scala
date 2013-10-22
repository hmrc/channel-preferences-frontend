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


class OtherServicesFactorySpec extends BaseSpec with MockitoSugar {

  val factory = new OtherServicesFactory {
    override def buildPortalUrl(destinationPathKey: String)(implicit request: Request[AnyRef], user: User): String = destinationPathKey
  }

  val linkToHmrcWebsite = "http://www.hmrc.gov.uk/online/new.htm#2"
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

      assertCorrectBusinessTaxRegistration(hmrcWebsiteLinkText)
    }

    "return a BusinessTaxRegistration object containing only the link to the hmrc website if epaye, vat and ct are defined" in {

      val regimes = RegimeRoots(ct = ctRoot, vat = vatRoot, epaye = epayeRoot)
      implicit val mockRequest = mock[Request[AnyRef]]
      implicit val user = User("userId", UserAuthority("userId", Regimes()), regimes, decryptedToken = None)

      assertCorrectBusinessTaxRegistration(hmrcWebsiteLinkText)
    }

    "return a BusinessTaxRegistration object containing only the link to the hmrc website if epaye, vat, sa and ct are defined" in {

      val regimes = RegimeRoots(sa = saRoot, ct = ctRoot, vat = vatRoot, epaye = epayeRoot)
      implicit val mockRequest = mock[Request[AnyRef]]
      implicit val user = User("userId", UserAuthority("userId", Regimes()), regimes, decryptedToken = None)

      assertCorrectBusinessTaxRegistration(hmrcWebsiteLinkText)
    }

    "return a BusinessTaxRegistration object containing registration link for all the regimes if none of the regimes are defined" in {

      val regimes = RegimeRoots()
      implicit val mockRequest = mock[Request[AnyRef]]
      implicit val user = User("userId", UserAuthority("userId", Regimes()), regimes, decryptedToken = None)

      assertCorrectBusinessTaxRegistration("Register for SA, CT, employers PAYE, or VAT")
    }

    "return a BusinessTaxRegistration object containing registration link for sa, ct and epaye regimes if only vat is defined" in {

      val regimes = RegimeRoots(vat = vatRoot)
      implicit val mockRequest = mock[Request[AnyRef]]
      implicit val user = User("userId", UserAuthority("userId", Regimes()), regimes, decryptedToken = None)

      assertCorrectBusinessTaxRegistration("Register for SA, CT, or employers PAYE")
    }

    "return a BusinessTaxRegistration object containing registration link for sa, ct and vat regimes if only epaye is defined" in {

      val regimes = RegimeRoots(epaye = epayeRoot)
      implicit val mockRequest = mock[Request[AnyRef]]
      implicit val user = User("userId", UserAuthority("userId", Regimes()), regimes, decryptedToken = None)

      assertCorrectBusinessTaxRegistration("Register for SA, CT, or VAT")
    }

    "return a BusinessTaxRegistration object containing registration link for sa and ct regimes if epaye and vat are defined" in {

      val regimes = RegimeRoots(epaye = epayeRoot, vat = vatRoot)
      implicit val mockRequest = mock[Request[AnyRef]]
      implicit val user = User("userId", UserAuthority("userId", Regimes()), regimes, decryptedToken = None)

      assertCorrectBusinessTaxRegistration("Register for SA, or CT")
    }

    "return a BusinessTaxRegistration object containing registration link for sa, epaye and vat if only ct is defined" in {

      val regimes = RegimeRoots(ct = ctRoot)
      implicit val mockRequest = mock[Request[AnyRef]]
      implicit val user = User("userId", UserAuthority("userId", Regimes()), regimes, decryptedToken = None)

      assertCorrectBusinessTaxRegistration("Register for SA, employers PAYE, or VAT")
    }

    "return a BusinessTaxRegistration object containing registration link for sa and epaye if ct and vat are defined" in {

      val regimes = RegimeRoots(ct = ctRoot, vat = vatRoot)
      implicit val mockRequest = mock[Request[AnyRef]]
      implicit val user = User("userId", UserAuthority("userId", Regimes()), regimes, decryptedToken = None)

      assertCorrectBusinessTaxRegistration("Register for SA, or employers PAYE")
    }

    "return a BusinessTaxRegistration object containing registration link for sa and vat if ct and epaye are defined" in {

      val regimes = RegimeRoots(ct = ctRoot, epaye = epayeRoot)
      implicit val mockRequest = mock[Request[AnyRef]]
      implicit val user = User("userId", UserAuthority("userId", Regimes()), regimes, decryptedToken = None)

      assertCorrectBusinessTaxRegistration("Register for SA, or VAT")
    }

    "return a BusinessTaxRegistration object containing registration link for ct, epaye and vat if only sa is defined" in {

      val regimes = RegimeRoots(sa = saRoot)
      implicit val mockRequest = mock[Request[AnyRef]]
      implicit val user = User("userId", UserAuthority("userId", Regimes()), regimes, decryptedToken = None)

      assertCorrectBusinessTaxRegistration("Register for CT, employers PAYE, or VAT")
    }

    "return a BusinessTaxRegistration object containing registration link for ct and epaye if sa and vat are defined" in {

      val regimes = RegimeRoots(sa = saRoot, vat=vatRoot)
      implicit val mockRequest = mock[Request[AnyRef]]
      implicit val user = User("userId", UserAuthority("userId", Regimes()), regimes, decryptedToken = None)

      assertCorrectBusinessTaxRegistration("Register for CT, or employers PAYE")
    }

    "return a BusinessTaxRegistration object containing registration link for ct and vat if sa and epaye are defined" in {

      val regimes = RegimeRoots(sa = saRoot, epaye=epayeRoot)
      implicit val mockRequest = mock[Request[AnyRef]]
      implicit val user = User("userId", UserAuthority("userId", Regimes()), regimes, decryptedToken = None)

      assertCorrectBusinessTaxRegistration("Register for CT, or VAT")
    }

    "return a BusinessTaxRegistration object containing registration link for employers paye and vat if sa and ct are defined" in {

      val regimes = RegimeRoots(sa = saRoot, ct=ctRoot)
      implicit val mockRequest = mock[Request[AnyRef]]
      implicit val user = User("userId", UserAuthority("userId", Regimes()), regimes, decryptedToken = None)

      assertCorrectBusinessTaxRegistration("Register for employers PAYE, or VAT")
    }

    "return a BusinessTaxRegistration object containing registration link for employers paye  if sa, ct and vat are defined" in {

      val regimes = RegimeRoots(sa = saRoot, ct=ctRoot, vat=vatRoot)
      implicit val mockRequest = mock[Request[AnyRef]]
      implicit val user = User("userId", UserAuthority("userId", Regimes()), regimes, decryptedToken = None)

      assertCorrectBusinessTaxRegistration("Register for employers PAYE")
    }

    "return a BusinessTaxRegistration object containing registration link for vat  if sa, ct and epaye are defined" in {

      val regimes = RegimeRoots(sa = saRoot, ct=ctRoot, epaye=epayeRoot)
      implicit val mockRequest = mock[Request[AnyRef]]
      implicit val user = User("userId", UserAuthority("userId", Regimes()), regimes, decryptedToken = None)

      assertCorrectBusinessTaxRegistration("Register for VAT")
    }
  }

  "the create function" should {
    "return an OtherServiceSummary" in {

      val regimes = RegimeRoots(sa = saRoot, ct=ctRoot, epaye=epayeRoot)
      implicit val mockRequest = mock[Request[AnyRef]]
      implicit val user = User("userId", UserAuthority("userId", Regimes()), regimes, decryptedToken = None)

      val expected = OtherServicesSummary(
        None,
        OnlineServicesEnrolment(RenderableLinkMessage(LinkMessage("otherServicesEnrolment", "here"))),
        BusinessTaxesRegistration(
          Some(RenderableLinkMessage(LinkMessage(linkToHmrcWebsite, "Register for VAT"))),
          RenderableLinkMessage(LinkMessage(linkToHmrcWebsite, hmrcWebsiteLinkText)))
      )

      factory.create(user) shouldBe expected

    }
  }

  private def assertCorrectBusinessTaxRegistration(linkMessage: String)(implicit user: User) {
    def linkObj = Some(RenderableLinkMessage(LinkMessage(linkToHmrcWebsite, linkMessage)))
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
