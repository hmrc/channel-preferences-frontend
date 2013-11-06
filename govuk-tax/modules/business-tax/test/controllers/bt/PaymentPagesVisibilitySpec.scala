package controllers.bt

import uk.gov.hmrc.common.BaseSpec
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.common.microservice.epaye.EpayeConnector
import org.mockito.Mockito._
import uk.gov.hmrc.common.microservice.epaye.domain._
import uk.gov.hmrc.domain.EmpRef
import play.api.test.FakeRequest
import uk.gov.hmrc.common.microservice.auth.domain.UserAuthority
import uk.gov.hmrc.common.microservice.auth.domain.Regimes
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.common.microservice.epaye.domain.EpayeAccountSummary
import uk.gov.hmrc.common.microservice.domain.RegimeRoots
import uk.gov.hmrc.common.microservice.epaye.domain.EpayeLinks
import uk.gov.hmrc.common.microservice.epaye.domain.RTI
import scala.Some

class PaymentPagesVisibilitySpec extends BaseSpec with MockitoSugar {

  "EpayePaymentPredicate" should {

    "be true when we can retrieve the details from the EPAYE connector for an RTI user " in {

      implicit val epayeConnectorMock = mock[EpayeConnector]
      val epayeRoot = Some(EpayeRoot(EmpRef("emp/6353"), EpayeLinks(Some("someUri"))))
      val user = User(userId = "userId", userAuthority = UserAuthority("userId", Regimes()), nameFromGovernmentGateway = Some("Ciccio"), regimes = RegimeRoots(epaye = epayeRoot), decryptedToken = None)

      when(epayeConnectorMock.accountSummary("someUri")).thenReturn(Some(EpayeAccountSummary(Some(RTI(35.38)), None)))

      val predicate = new EpayePaymentPredicate(epayeConnectorMock)

      predicate.isVisible(user, FakeRequest()) shouldBe true
      verify(epayeConnectorMock).accountSummary("someUri")

    }

    "be true when we can retrieve the details from the EPAYE connector for a non-RTI user " in {

      implicit val epayeConnectorMock = mock[EpayeConnector]
      val epayeRoot = Some(EpayeRoot(EmpRef("emp/6353"), EpayeLinks(Some("someUri"))))
      val user = User(userId = "userId", userAuthority = UserAuthority("userId", Regimes()), nameFromGovernmentGateway = Some("Ciccio"), regimes = RegimeRoots(epaye = epayeRoot), decryptedToken = None)

      when(epayeConnectorMock.accountSummary("someUri")).thenReturn(Some(EpayeAccountSummary(None, Some(NonRTI(736, 2013)))))

      val predicate = new EpayePaymentPredicate(epayeConnectorMock)

      predicate.isVisible(user, FakeRequest()) shouldBe true
      verify(epayeConnectorMock).accountSummary("someUri")

    }

    "be false when we cannot retrieve the details from the EPAYE connector " in {

      implicit val epayeConnectorMock = mock[EpayeConnector]
      val epayeRoot = Some(EpayeRoot(EmpRef("emp/6353"), EpayeLinks(Some("someUri"))))
      val user = User(userId = "userId", userAuthority = UserAuthority("userId", Regimes()), nameFromGovernmentGateway = Some("Ciccio"), regimes = RegimeRoots(epaye = epayeRoot), decryptedToken = None)

      when(epayeConnectorMock.accountSummary("someUri")).thenReturn(Some(EpayeAccountSummary(None, None)))

      val predicate = new EpayePaymentPredicate(epayeConnectorMock)

      predicate.isVisible(user, FakeRequest()) shouldBe false
      verify(epayeConnectorMock).accountSummary("someUri")

    }

  }


}
