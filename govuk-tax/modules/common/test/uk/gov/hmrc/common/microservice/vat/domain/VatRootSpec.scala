package uk.gov.hmrc.common.microservice.vat.domain

import uk.gov.hmrc.common.BaseSpec
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import uk.gov.hmrc.domain.Vrn
import uk.gov.hmrc.common.microservice.vat.VatConnector
import scala.concurrent._

class VatRootSpec extends BaseSpec with MockitoSugar {

  private val vrn = Vrn("123456789")
  private val accountSummaryLink = s"/vat/$vrn/accountSummary"
  private val accountSummary = VatAccountSummary(accountBalance = Some(VatAccountBalance(Some(50.4D))), dateOfBalance = Some("2013-11-22"))

  "Requesting AccountSummary" should {

    "return an AccountSummary object if the service call is successful" in {
      val mockConnector = mock[VatConnector]
      val root = VatRoot(vrn, Map("accountSummary" -> accountSummaryLink))
      when(mockConnector.accountSummary(accountSummaryLink)).thenReturn(Future.successful(Some(accountSummary)))
      await(root.accountSummary(mockConnector, hc)) shouldBe Some(accountSummary)
    }

    "return None if no accountSummary link exists" in {
      val mockConnector = mock[VatConnector]
      val root = VatRoot(vrn, Map[String, String]())
      await(root.accountSummary(mockConnector, hc)) shouldBe None
      verifyZeroInteractions(mockConnector)
    }

    "throw an exception if we have an accountSummary link but it returns a not found status" in {
      val mockConnector = mock[VatConnector]
      val root = VatRoot(vrn, Map("accountSummary" -> accountSummaryLink))
      when(mockConnector.accountSummary(accountSummaryLink)).thenReturn(Future.successful(None))

      val thrown = evaluating(await(root.accountSummary(mockConnector, hc)) shouldBe Some(accountSummary)) should produce[IllegalStateException]

      thrown.getMessage shouldBe s"Expected HOD data not found for link 'accountSummary' with path: $accountSummaryLink"
    }

    "propagate an exception if thrown by the external connector while requesting the accountSummary" in {
      val mockConnector = mock[VatConnector]
      val root = VatRoot(vrn, Map("accountSummary" -> accountSummaryLink))
      when(mockConnector.accountSummary(accountSummaryLink)).thenThrow(new NumberFormatException("Not a number"))

      evaluating(root.accountSummary(mockConnector, hc) shouldBe Some(accountSummary)) should produce[NumberFormatException]
    }
  }

}
