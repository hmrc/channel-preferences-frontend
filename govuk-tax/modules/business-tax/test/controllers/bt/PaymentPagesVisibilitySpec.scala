package controllers.bt

import uk.gov.hmrc.common.BaseSpec
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.common.microservice.epaye.EpayeConnector
import org.mockito.Mockito._
import uk.gov.hmrc.common.microservice.epaye.domain._
import uk.gov.hmrc.domain.EmpRef
import play.api.test.FakeRequest
import uk.gov.hmrc.common.microservice.sa.domain.SaRoot
import uk.gov.hmrc.common.microservice.sa.SaConnector
import uk.gov.hmrc.common.microservice.ct.domain.CtRoot
import uk.gov.hmrc.common.microservice.ct.CtConnector
import uk.gov.hmrc.common.microservice.vat.VatConnector
import uk.gov.hmrc.common.microservice.vat.domain.VatRoot
import controllers.domain.AuthorityUtils._
import uk.gov.hmrc.common.microservice.epaye.domain.EpayeLinks
import uk.gov.hmrc.common.microservice.epaye.domain.NonRTI
import uk.gov.hmrc.common.microservice.vat.domain.VatAccountSummary
import scala.Some
import uk.gov.hmrc.domain.Vrn
import uk.gov.hmrc.common.microservice.vat.domain.VatAccountBalance
import uk.gov.hmrc.domain.CtUtr
import uk.gov.hmrc.common.microservice.sa.domain.SaAccountSummary
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.common.microservice.epaye.domain.EpayeAccountSummary
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.common.microservice.domain.RegimeRoots
import uk.gov.hmrc.common.microservice.epaye.domain.RTI
import uk.gov.hmrc.common.microservice.ct.domain.CtAccountSummary
import uk.gov.hmrc.common.microservice.ct.domain.CtAccountBalance

class PaymentPagesVisibilitySpec extends BaseSpec with MockitoSugar {

  "EpayePaymentPredicate" should {

    "be true when we can retrieve the details from the EPAYE connector for an RTI user " in {

      implicit val epayeConnectorMock = mock[EpayeConnector]
      val epayeRoot = Some(EpayeRoot(EmpRef("emp/6353"), EpayeLinks(Some("someUri"))))
      val user = User(userId = "userId", userAuthority = epayeAuthority("userId", "emp/6353"), nameFromGovernmentGateway = Some("Ciccio"), regimes = RegimeRoots(epaye = epayeRoot), decryptedToken = None)

      when(epayeConnectorMock.accountSummary("someUri")).thenReturn(Some(EpayeAccountSummary(Some(RTI(35.38)), None)))

      val predicate = new EpayePaymentPredicate(epayeConnectorMock)

      await(predicate.isVisible(user, FakeRequest())) shouldBe true
      verify(epayeConnectorMock).accountSummary("someUri")

    }

    "be true when we can retrieve the details from the EPAYE connector for a non-RTI user " in {

      implicit val epayeConnectorMock = mock[EpayeConnector]
      val epayeRoot = Some(EpayeRoot(EmpRef("emp/6353"), EpayeLinks(Some("someUri"))))
      val user = User(userId = "userId", userAuthority = epayeAuthority("userId", "emp/6353"), nameFromGovernmentGateway = Some("Ciccio"), regimes = RegimeRoots(epaye = epayeRoot), decryptedToken = None)

      when(epayeConnectorMock.accountSummary("someUri")).thenReturn(Some(EpayeAccountSummary(None, Some(NonRTI(736, 2013)))))

      val predicate = new EpayePaymentPredicate(epayeConnectorMock)

      await(predicate.isVisible(user, FakeRequest())) shouldBe true
      verify(epayeConnectorMock).accountSummary("someUri")

    }

    "be false when we cannot retrieve the link from the root to retrieve the EPAYE connector " in {

      implicit val epayeConnectorMock = mock[EpayeConnector]
      val epayeRoot = Some(EpayeRoot(EmpRef("emp/6353"), EpayeLinks(None)))
      val user = User(userId = "userId", userAuthority = epayeAuthority("userId", "emp/6353"), nameFromGovernmentGateway = Some("Ciccio"), regimes = RegimeRoots(epaye = epayeRoot), decryptedToken = None)

      val predicate = new EpayePaymentPredicate(epayeConnectorMock)

      await(predicate.isVisible(user, FakeRequest())) shouldBe false
      verifyZeroInteractions(epayeConnectorMock)

    }

  }

  "SaPaymentPredicate" should {

    "be true when we can retrieve the details from the SA connector for a user " in {

      implicit val saConnectorMock = mock[SaConnector]
      val saRoot = Some(SaRoot(SaUtr("saUtr"), Map("individual/account-summary"->"someUri")))
      val user = User(userId = "userId", userAuthority = saAuthority("userId", "saUtr"), nameFromGovernmentGateway = Some("Ciccio"), regimes = RegimeRoots(sa = saRoot), decryptedToken = None)

      when(saConnectorMock.accountSummary("someUri")).thenReturn(Some(SaAccountSummary(None, None, None)))

      val predicate = new SaPaymentPredicate(saConnectorMock)

      await(predicate.isVisible(user, FakeRequest())) shouldBe true
      verify(saConnectorMock).accountSummary("someUri")

    }

    "be false when we cannot retrieve the link from the root to retrieve the SA account summary for a user " in {

      implicit val saConnectorMock = mock[SaConnector]
      val saRoot = Some(SaRoot(SaUtr("saUtr"), links = Map.empty[String, String]))
      val user = User(userId = "userId", userAuthority = saAuthority("userId", "saUtr"), nameFromGovernmentGateway = Some("Ciccio"), regimes = RegimeRoots(sa = saRoot), decryptedToken = None)


      val predicate = new SaPaymentPredicate(saConnectorMock)

      await(predicate.isVisible(user, FakeRequest())) shouldBe false
      verifyZeroInteractions(saConnectorMock)

    }

  }

  "CtPaymentPredicate" should {

    "be true when we can retrieve the details from the CT connector for a user " in {

      implicit val ctConnectorMock = mock[CtConnector]
      val ctRoot = Some(CtRoot(CtUtr("ctUtr"), Map("accountSummary"->"someUri")))
      val user = User(userId = "userId", userAuthority = ctAuthority("userId", "ctUtr"), nameFromGovernmentGateway = Some("Ciccio"), regimes = RegimeRoots(ct = ctRoot), decryptedToken = None)

      when(ctConnectorMock.accountSummary("someUri")).thenReturn(Some(CtAccountSummary(Some(CtAccountBalance(Some(3222))), Some("2013-03-23"))))

      val predicate = new CtPaymentPredicate(ctConnectorMock)

      await(predicate.isVisible(user, FakeRequest())) shouldBe true
      verify(ctConnectorMock).accountSummary("someUri")

    }

    "be false when we cannot retrieve the link from the root to retrieve the CT account summary for a user " in {

      implicit val ctConnectorMock = mock[CtConnector]
      val ctRoot = Some(CtRoot(CtUtr("ctUtr"), Map("anyOtherKey"->"someUri")))
      val user = User(userId = "userId", userAuthority = ctAuthority("userId", "ctUtr"), nameFromGovernmentGateway = Some("Ciccio"), regimes = RegimeRoots(ct = ctRoot), decryptedToken = None)

      val predicate = new CtPaymentPredicate(ctConnectorMock)

      await(predicate.isVisible(user, FakeRequest())) shouldBe false
      verifyZeroInteractions(ctConnectorMock)

    }

    "be false when we retrieve the account summary from the connector but the account balance is missing " in {

      implicit val ctConnectorMock = mock[CtConnector]
      val ctRoot = Some(CtRoot(CtUtr("ctUtr"), Map("accountSummary"->"someUri")))
      val user = User(userId = "userId", userAuthority = ctAuthority("userId", "ctUtr"), nameFromGovernmentGateway = Some("Ciccio"), regimes = RegimeRoots(ct = ctRoot), decryptedToken = None)

      when(ctConnectorMock.accountSummary("someUri")).thenReturn(Some(CtAccountSummary(None, Some("2013-03-23"))))

      val predicate = new CtPaymentPredicate(ctConnectorMock)

      await(predicate.isVisible(user, FakeRequest())) shouldBe false
      verify(ctConnectorMock).accountSummary("someUri")

    }

    "be false when we retrieve the account summary from the connector but the date of balance is missing " in {

      implicit val ctConnectorMock = mock[CtConnector]
      val ctRoot = Some(CtRoot(CtUtr("ctUtr"), Map("accountSummary"->"someUri")))
      val user = User(userId = "userId", userAuthority = ctAuthority("userId", "ctUtr"), nameFromGovernmentGateway = Some("Ciccio"), regimes = RegimeRoots(ct = ctRoot), decryptedToken = None)

      when(ctConnectorMock.accountSummary("someUri")).thenReturn(Some(CtAccountSummary(Some(CtAccountBalance(Some(3222))), None)))

      val predicate = new CtPaymentPredicate(ctConnectorMock)

      await(predicate.isVisible(user, FakeRequest())) shouldBe false
      verify(ctConnectorMock).accountSummary("someUri")

    }

    "be false when we retrieve the account summary from the connector but the account balance and date of balance are missing " in {

      implicit val ctConnectorMock = mock[CtConnector]
      val ctRoot = Some(CtRoot(CtUtr("ctUtr"), Map("accountSummary"->"someUri")))
      val user = User(userId = "userId", userAuthority = ctAuthority("userId", "ctUtr"), nameFromGovernmentGateway = Some("Ciccio"), regimes = RegimeRoots(ct = ctRoot), decryptedToken = None)

      when(ctConnectorMock.accountSummary("someUri")).thenReturn(Some(CtAccountSummary(None, None)))

      val predicate = new CtPaymentPredicate(ctConnectorMock)

      await(predicate.isVisible(user, FakeRequest())) shouldBe false
      verify(ctConnectorMock).accountSummary("someUri")

    }

  }

  "VatPaymentPredicate" should {

    "be true when we can retrieve the details from the VAT connector for a user " in {

      implicit val vatConnectorMock = mock[VatConnector]
      val vatRoot = Some(VatRoot(Vrn("vrn"), Map("accountSummary"->"someUri")))
      val user = User(userId = "userId", userAuthority = vatAuthority("userId", "vrn"), nameFromGovernmentGateway = Some("Ciccio"), regimes = RegimeRoots(vat = vatRoot), decryptedToken = None)

      when(vatConnectorMock.accountSummary("someUri")).thenReturn(Some(VatAccountSummary(Some(VatAccountBalance(Some(333))), Some("2013-08-23"))))

      val predicate = new VatPaymentPredicate(vatConnectorMock)

      await(predicate.isVisible(user, FakeRequest())) shouldBe true
      verify(vatConnectorMock).accountSummary("someUri")

    }

    "be false when we cannot retrieve the link from the root to retrieve the VAT account summary for a user " in {

      implicit val vatConnectorMock = mock[VatConnector]
      val vatRoot = Some(VatRoot(Vrn("vrn"), Map.empty[String, String]))
      val user = User(userId = "userId", userAuthority = vatAuthority("userId", "vrn"), nameFromGovernmentGateway = Some("Ciccio"), regimes = RegimeRoots(vat = vatRoot), decryptedToken = None)

      val predicate = new VatPaymentPredicate(vatConnectorMock)

      await(predicate.isVisible(user, FakeRequest())) shouldBe false
      verifyZeroInteractions(vatConnectorMock)

    }


    "be false when we can retrieve the details from the VAT connector for a user but the account balance is missing " in {

      implicit val vatConnectorMock = mock[VatConnector]
      val vatRoot = Some(VatRoot(Vrn("vrn"), Map("accountSummary"->"someUri")))
      val user = User(userId = "userId", userAuthority = vatAuthority("userId", "vrn"), nameFromGovernmentGateway = Some("Ciccio"), regimes = RegimeRoots(vat = vatRoot), decryptedToken = None)

      when(vatConnectorMock.accountSummary("someUri")).thenReturn(Some(VatAccountSummary(Some(VatAccountBalance(None)), Some("2013-08-23"))))

      val predicate = new VatPaymentPredicate(vatConnectorMock)

      await(predicate.isVisible(user, FakeRequest())) shouldBe false
      verify(vatConnectorMock).accountSummary("someUri")

    }

    "be false when we can retrieve the details from the VAT connector for a user but the date of balance is missing " in {

      implicit val vatConnectorMock = mock[VatConnector]
      val vatRoot = Some(VatRoot(Vrn("vrn"), Map("accountSummary"->"someUri")))
      val user = User(userId = "userId", userAuthority = vatAuthority("userId", "vrn"), nameFromGovernmentGateway = Some("Ciccio"), regimes = RegimeRoots(vat = vatRoot), decryptedToken = None)

      when(vatConnectorMock.accountSummary("someUri")).thenReturn(Some(VatAccountSummary(Some(VatAccountBalance(Some(323))), None)))

      val predicate = new VatPaymentPredicate(vatConnectorMock)

      await(predicate.isVisible(user, FakeRequest())) shouldBe false
      verify(vatConnectorMock).accountSummary("someUri")

    }

    "be false when we can retrieve the details from the VAT connector for a user but the account balance and the date of balance are missing " in {

      implicit val vatConnectorMock = mock[VatConnector]
      val vatRoot = Some(VatRoot(Vrn("vrn"), Map("accountSummary"->"someUri")))
      val user = User(userId = "userId", userAuthority = vatAuthority("userId", "vrn"), nameFromGovernmentGateway = Some("Ciccio"), regimes = RegimeRoots(vat = vatRoot), decryptedToken = None)

      when(vatConnectorMock.accountSummary("someUri")).thenReturn(Some(VatAccountSummary(None, None)))

      val predicate = new VatPaymentPredicate(vatConnectorMock)

      await(predicate.isVisible(user, FakeRequest())) shouldBe false
      verify(vatConnectorMock).accountSummary("someUri")

    }
  }

}
