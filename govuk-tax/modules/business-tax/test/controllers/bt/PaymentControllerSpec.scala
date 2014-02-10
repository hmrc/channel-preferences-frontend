package controllers.bt

import uk.gov.hmrc.common.BaseSpec
import uk.gov.hmrc.common.microservice.sa.domain.SaRoot
import uk.gov.hmrc.domain.EmpRef
import play.api.test.{FakeRequest, WithApplication}
import scala.concurrent.Future
import play.api.test.Helpers._
import uk.gov.hmrc.common.microservice.epaye.domain.EpayeRoot
import uk.gov.hmrc.common.microservice.ct.domain.CtRoot
import uk.gov.hmrc.common.microservice.vat.domain.VatRoot
import controllers.domain.AuthorityUtils._
import uk.gov.hmrc.domain.CtUtr
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.common.microservice.domain.RegimeRoots
import uk.gov.hmrc.common.microservice.epaye.domain.EpayeLinks
import play.api.test.FakeApplication
import scala.Some
import uk.gov.hmrc.domain.Vrn
import controllers.common.SessionKeys

class PaymentControllerSpec extends BaseSpec {


  "render the SA Make a Payment page" should {
    "return the sa payment page succesfully" in new WithApplication(FakeApplication()) {

      val controllerUnderTest = new PaymentController

      val saRoot = Some(SaRoot(SaUtr("sa-utr"), Map.empty[String, String]))
      val user = User(userId = "userId", userAuthority = saAuthority("userId", "sa-utr"), nameFromGovernmentGateway = Some("Ciccio"), regimes = RegimeRoots(sa = saRoot), decryptedToken = None)
      val request = FakeRequest().withSession(SessionKeys.affinityGroup -> "sa")

      val result = Future.successful(controllerUnderTest.makeSaPaymentPage(user, request))

      status(result) shouldBe 200
    }
  }

  "render the EPAYE Make a Payment page" should {
    "return the epaye payment page succesfully" in new WithApplication(FakeApplication()){

      val controllerUnderTest = new PaymentController
      val epayeRoot = Some(EpayeRoot(EmpRef("emp/ref"), EpayeLinks(None)))

      val user = User(userId = "userId", userAuthority = epayeAuthority("userId", "emp/ref"), nameFromGovernmentGateway = Some("Ciccio"), regimes = RegimeRoots(epaye = epayeRoot), decryptedToken = None)
      val request = FakeRequest().withSession(SessionKeys.affinityGroup -> "epaye")

      val result = Future.successful(controllerUnderTest.makeEpayePaymentPage(user, request))

      status(result) shouldBe 200
    }
  }

  "render the CT Make a Payment page" should {
    "return the ct payment page successfully" in new WithApplication(FakeApplication()){

      val controllerUnderTest = new PaymentController
      val ctRoot = Some(CtRoot(CtUtr("ct-utr"), Map.empty[String, String]))

      val user = User(userId = "userId", userAuthority = ctAuthority("userId", "ct-utr"), nameFromGovernmentGateway = Some("Ciccio"), regimes = RegimeRoots(ct = ctRoot), decryptedToken = None)
      val request = FakeRequest().withSession(SessionKeys.affinityGroup -> "ct")

      val result = Future.successful(controllerUnderTest.makeCtPaymentPage(user, request))

      status(result) shouldBe 200
    }
  }

  "render the VAT Make a Payment page" should {
    "return the vat payment page succesfully" in new WithApplication(FakeApplication()){

      val controllerUnderTest = new PaymentController
      val vatRoot = Some(VatRoot(Vrn("vrn"), Map.empty[String, String]))

      val user = User(userId = "userId", userAuthority = vatAuthority("userId", "vrn"), nameFromGovernmentGateway = Some("Ciccio"), regimes = RegimeRoots(vat = vatRoot), decryptedToken = None)
      val request = FakeRequest().withSession(SessionKeys.affinityGroup -> "vat")

      val result = Future.successful(controllerUnderTest.makeVatPaymentPage(user, request))

      status(result) shouldBe 200
    }
  }

}
