package uk.gov.hmrc.common.microservice.epaye.domain

import uk.gov.hmrc.domain.EmpRef
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.common.BaseSpec
import uk.gov.hmrc.common.microservice.epaye.EpayeConnector
import scala.concurrent._
import duration._
import controllers.common.actions.HeaderCarrier


class EpayeRootSpec extends BaseSpec with MockitoSugar {

  private val empRef = EmpRef("ABC", "2345X")
  private val accountSummaryLink = s"/epaye/$empRef/accountSummary"
  private val accountSummary = EpayeAccountSummary(rti = Some(RTI(45.3)))

  "Requesting AccountSummary" should {

    "return an AccountSummary object if the service call is successful" in {
      val mockConnector = mock[EpayeConnector]
      val root = EpayeRoot(empRef, EpayeLinks(Some(accountSummaryLink)))
      when(mockConnector.accountSummary(accountSummaryLink)).thenReturn(Some(accountSummary))
      await(root.accountSummary(mockConnector, hc)) shouldBe Some(accountSummary)
    }

    "return None if no accountSummary link exists" in {
      val mockConnector = mock[EpayeConnector]
      val root = EpayeRoot(empRef, EpayeLinks(None))
      await(root.accountSummary(mockConnector, hc)) shouldBe None
      verifyZeroInteractions(mockConnector)
    }

    "throw an exception if we have an accountSummary link but it returns a not found status" in {
      val mockConnector = mock[EpayeConnector]
      val root = EpayeRoot(empRef, EpayeLinks(Some(accountSummaryLink)))
      when(mockConnector.accountSummary(accountSummaryLink)).thenReturn(None)

      val thrown = evaluating(await(root.accountSummary(mockConnector, hc)) shouldBe Some(accountSummary)) should produce [IllegalStateException]

      thrown.getMessage shouldBe s"Expected HOD data not found for link 'accountSummary' with path: $accountSummaryLink"
    }

    "propagate an exception if thrown by the external connector while requesting the accountSummary" in {
      val mockConnector = mock[EpayeConnector]
      val root = EpayeRoot(empRef, EpayeLinks(Some(accountSummaryLink)))
      when(mockConnector.accountSummary(accountSummaryLink)).thenThrow(new NumberFormatException("Not a number"))

      evaluating(root.accountSummary(mockConnector, hc) shouldBe Some(accountSummary)) should produce [NumberFormatException]
    }
  }
}
