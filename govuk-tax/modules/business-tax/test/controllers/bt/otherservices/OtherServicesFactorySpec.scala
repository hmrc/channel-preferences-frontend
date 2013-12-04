package controllers.bt.otherservices

import uk.gov.hmrc.common.BaseSpec
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.domain.EmpRef
import play.api.test.WithApplication
import uk.gov.hmrc.common.microservice.sa.domain.SaRoot
import uk.gov.hmrc.common.microservice.ct.domain.CtRoot
import uk.gov.hmrc.common.microservice.vat.domain.VatRoot
import uk.gov.hmrc.common.microservice.epaye.domain.EpayeRoot
import org.scalatest.concurrent.ScalaFutures
import scala._
import uk.gov.hmrc.common.microservice.epaye.domain.EpayeLinks
import scala.Some
import uk.gov.hmrc.domain.Vrn
import uk.gov.hmrc.domain.CtUtr
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.common.microservice.domain.RegimeRoots
import play.api.test.FakeApplication


class OtherServicesFactorySpec extends BaseSpec with MockitoSugar with ScalaFutures {

  import controllers.domain.AuthorityUtils._
  
  "createBusinessTaxesRegistration" should {
    "return a BusinessTaxRegistration object containing only the link to the hmrc website if epaye, vat and sa are defined" in new OtherServicesFactoryForTest {

      val user = User("userId", emptyAuthority("userId"), RegimeRoots(sa = saRoot, vat = vatRoot, epaye = epayeRoot), decryptedToken = None)
      OtherServicesFactory.createRegistrationText(user) shouldBe None
    }

    "return a BusinessTaxRegistration object containing only the link to the hmrc website if epaye, vat and ct are defined" in new OtherServicesFactoryForTest {

      val regimes = RegimeRoots(ct = ctRoot, vat = vatRoot, epaye = epayeRoot)

      implicit val user = User("userId", emptyAuthority("userId"), regimes, decryptedToken = None)

      OtherServicesFactory.createRegistrationText(user) shouldBe None
    }

    "return a BusinessTaxRegistration object containing only the link to the hmrc website if epaye, vat, sa and ct are defined" in new OtherServicesFactoryForTest {

      val regimes = RegimeRoots(sa = saRoot, ct = ctRoot, vat = vatRoot, epaye = epayeRoot)

      implicit val user = User("userId", emptyAuthority("userId"), regimes, decryptedToken = None)
      OtherServicesFactory.createRegistrationText(user) shouldBe None
    }

    "return a BusinessTaxRegistration object containing registration link for all the regimes if none of the regimes are defined" in new OtherServicesFactoryForTest {

      val regimes = RegimeRoots()

      implicit val user = User("userId", emptyAuthority("userId"), regimes, decryptedToken = None)

      OtherServicesFactory.createRegistrationText(user) shouldBe Some("Register for SA, CT, employers PAYE, or VAT")
    }

    "return a BusinessTaxRegistration object containing registration link for sa, ct and epaye regimes if only vat is defined" in new OtherServicesFactoryForTest {

      val regimes = RegimeRoots(vat = vatRoot)

      implicit val user = User("userId", emptyAuthority("userId"), regimes, decryptedToken = None)

      OtherServicesFactory.createRegistrationText(user) shouldBe Some("Register for SA, CT, or employers PAYE")
    }

    "return a BusinessTaxRegistration object containing registration link for sa, ct and vat regimes if only epaye is defined" in new OtherServicesFactoryForTest {

      val regimes = RegimeRoots(epaye = epayeRoot)

      implicit val user = User("userId", emptyAuthority("userId"), regimes, decryptedToken = None)

      OtherServicesFactory.createRegistrationText(user) shouldBe Some("Register for SA, CT, or VAT")
    }

    "return a BusinessTaxRegistration object containing registration link for sa and ct regimes if epaye and vat are defined" in new OtherServicesFactoryForTest {

      val regimes = RegimeRoots(epaye = epayeRoot, vat = vatRoot)

      implicit val user = User("userId", emptyAuthority("userId"), regimes, decryptedToken = None)

      OtherServicesFactory.createRegistrationText(user) shouldBe Some("Register for SA, or CT")
    }

    "return a BusinessTaxRegistration object containing registration link for sa, epaye and vat if only ct is defined" in new OtherServicesFactoryForTest {

      val regimes = RegimeRoots(ct = ctRoot)

      implicit val user = User("userId", emptyAuthority("userId"), regimes, decryptedToken = None)

      OtherServicesFactory.createRegistrationText(user) shouldBe Some("Register for SA, employers PAYE, or VAT")
    }

    "return a BusinessTaxRegistration object containing registration link for sa and epaye if ct and vat are defined" in new OtherServicesFactoryForTest {

      val regimes = RegimeRoots(ct = ctRoot, vat = vatRoot)

      implicit val user = User("userId", emptyAuthority("userId"), regimes, decryptedToken = None)

      OtherServicesFactory.createRegistrationText(user) shouldBe Some("Register for SA, or employers PAYE")
    }

    "return a BusinessTaxRegistration object containing registration link for sa and vat if ct and epaye are defined" in new OtherServicesFactoryForTest {

      val regimes = RegimeRoots(ct = ctRoot, epaye = epayeRoot)

      implicit val user = User("userId", emptyAuthority("userId"), regimes, decryptedToken = None)

      OtherServicesFactory.createRegistrationText(user) shouldBe Some("Register for SA, or VAT")
    }

    "return a BusinessTaxRegistration object containing registration link for ct, epaye and vat if only sa is defined" in new OtherServicesFactoryForTest {

      val regimes = RegimeRoots(sa = saRoot)

      implicit val user = User("userId", emptyAuthority("userId"), regimes, decryptedToken = None)

      OtherServicesFactory.createRegistrationText(user) shouldBe Some("Register for CT, employers PAYE, or VAT")
    }

    "return a BusinessTaxRegistration object containing registration link for ct and epaye if sa and vat are defined" in new OtherServicesFactoryForTest {

      val regimes = RegimeRoots(sa = saRoot, vat = vatRoot)

      implicit val user = User("userId", emptyAuthority("userId"), regimes, decryptedToken = None)

      OtherServicesFactory.createRegistrationText(user) shouldBe Some("Register for CT, or employers PAYE")
    }

    "return a BusinessTaxRegistration object containing registration link for ct and vat if sa and epaye are defined" in new OtherServicesFactoryForTest {

      val regimes = RegimeRoots(sa = saRoot, epaye = epayeRoot)

      implicit val user = User("userId", emptyAuthority("userId"), regimes, decryptedToken = None)

      OtherServicesFactory.createRegistrationText(user) shouldBe Some("Register for CT, or VAT")
    }

    "return a BusinessTaxRegistration object containing registration link for employers paye and vat if sa and ct are defined" in new OtherServicesFactoryForTest {

      val regimes = RegimeRoots(sa = saRoot, ct = ctRoot)

      implicit val user = User("userId", emptyAuthority("userId"), regimes, decryptedToken = None)

      OtherServicesFactory.createRegistrationText(user) shouldBe Some("Register for employers PAYE, or VAT")
    }

    "return a BusinessTaxRegistration object containing registration link for employers paye  if sa, ct and vat are defined" in new OtherServicesFactoryForTest {

      val regimes = RegimeRoots(sa = saRoot, ct = ctRoot, vat = vatRoot)

      implicit val user = User("userId", emptyAuthority("userId"), regimes, decryptedToken = None)

      OtherServicesFactory.createRegistrationText(user) shouldBe Some("Register for employers PAYE")
    }

    "return a BusinessTaxRegistration object containing registration link for vat  if sa, ct and epaye are defined" in new OtherServicesFactoryForTest {

      val regimes = RegimeRoots(sa = saRoot, ct = ctRoot, epaye = epayeRoot)
      implicit val user = User("userId", emptyAuthority("userId"), regimes, decryptedToken = None)

      OtherServicesFactory.createRegistrationText(user) shouldBe Some("Register for VAT")
    }
  }

  private def saRoot = Some(SaRoot(SaUtr("sa-utr"), Map.empty[String, String]))

  private def ctRoot = Some(CtRoot(CtUtr("ct-utr"), Map.empty[String, String]))

  private def epayeRoot = Some(EpayeRoot(EmpRef("emp/ref"), EpayeLinks(None)))

  private def vatRoot = Some(VatRoot(Vrn("vrn"), Map.empty[String, String]))
}

abstract class OtherServicesFactoryForTest extends WithApplication(FakeApplication())

