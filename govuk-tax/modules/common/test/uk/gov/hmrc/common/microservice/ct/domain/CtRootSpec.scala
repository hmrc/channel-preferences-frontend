package uk.gov.hmrc.common.microservice.ct.domain

import uk.gov.hmrc.common.BaseSpec
import uk.gov.hmrc.common.microservice.ct.CtConnector
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.domain.CtUtr
import org.mockito.Mockito._

class CtRootSpec extends BaseSpec with MockitoSugar {

  private val utr = CtUtr("2222233333")
  private val accountSummaryLink = s"/ct/$utr/accountSummary"
  private val accountSummary = CtAccountSummary(accountBalance = Some(CtAccountBalance(Some(50.4D))), dateOfBalance = Some("2013-11-22"))

  "Requesting AccountSummary" should {

    "return an AccountSummary object if the service call is successful" in {
      val mockConnector = mock[CtConnector]
      val root = CtRoot(utr, Map("accountSummary" -> accountSummaryLink))
      when(mockConnector.accountSummary(accountSummaryLink)).thenReturn(Some(accountSummary))
      root.accountSummary(mockConnector) shouldBe Some(accountSummary)

    }

    "return None if no accountSummary link exists" in {
      val mockConnector = mock[CtConnector]
      val root = CtRoot(utr, Map[String, String]())
      root.accountSummary(mockConnector) shouldBe None
      verifyZeroInteractions(mockConnector)
    }

    "throw an exception if we have an accountSummary link but it returns a not found status" in {
      val mockConnector = mock[CtConnector]
      val root = CtRoot(utr, Map("accountSummary" -> accountSummaryLink))
      when(mockConnector.accountSummary(accountSummaryLink)).thenReturn(None)

      val thrown = evaluating(root.accountSummary(mockConnector) shouldBe Some(accountSummary)) should produce [IllegalStateException]

      thrown.getMessage shouldBe s"Expected HOD data not found for link 'accountSummary' with path: $accountSummaryLink"
    }

    "propagate an exception if thrown by the external connector while requesting the accountSummary" in {
      val mockConnector = mock[CtConnector]
      val root = CtRoot(utr, Map("accountSummary" -> accountSummaryLink))
      when(mockConnector.accountSummary(accountSummaryLink)).thenThrow(new NumberFormatException("Not a number"))

      evaluating(root.accountSummary(mockConnector) shouldBe Some(accountSummary)) should produce [NumberFormatException]
    }
  }
}
