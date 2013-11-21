package uk.gov.hmrc.common.microservice.ct.domain

import uk.gov.hmrc.common.BaseSpec
import uk.gov.hmrc.common.microservice.ct.CtConnector
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.domain.CtUtr
import org.mockito.Mockito._
import controllers.common.actions.HeaderCarrier
import scala.concurrent.Future

class CtRootSpec extends BaseSpec with MockitoSugar {

  private val utr = CtUtr("2222233333")
  private val accountSummaryLink = s"/ct/$utr/accountSummary"
  private val accountSummary = CtAccountSummary(accountBalance = Some(CtAccountBalance(Some(50.4D))), dateOfBalance = Some("2013-11-22"))

  implicit val hc = HeaderCarrier()

  "Requesting AccountSummary" should {

    "return an AccountSummary object if the service call is successful" in {
      val mockConnector = mock[CtConnector]
      val root = CtRoot(utr, Map("accountSummary" -> accountSummaryLink))

      when(mockConnector.accountSummary(accountSummaryLink)).thenReturn(Future.successful(Some(accountSummary)))
      await(root.accountSummary(mockConnector, hc)) shouldBe Some(accountSummary)

    }

    "return None if no accountSummary link exists" in {
      val mockConnector = mock[CtConnector]
      val root = CtRoot(utr, Map[String, String]())
      await(root.accountSummary(mockConnector, hc)) shouldBe None
      verifyZeroInteractions(mockConnector)
    }

    "throw an exception if we have an accountSummary link but it returns a not found status" in {
      val mockConnector = mock[CtConnector]
      val root = CtRoot(utr, Map("accountSummary" -> accountSummaryLink))
      when(mockConnector.accountSummary(accountSummaryLink)).thenReturn(Future.successful(None))

      val thrown = evaluating(await(root.accountSummary(mockConnector, hc)) shouldBe Some(accountSummary)) should produce[IllegalStateException]

      thrown.getMessage shouldBe s"Expected HOD data not found for link 'accountSummary' with path: $accountSummaryLink"
    }

    "propagate an exception if thrown by the external connector while requesting the accountSummary" in {
      val mockConnector = mock[CtConnector]
      val root = CtRoot(utr, Map("accountSummary" -> accountSummaryLink))
      when(mockConnector.accountSummary(accountSummaryLink)).thenThrow(new NumberFormatException("Not a number"))

      evaluating(await(root.accountSummary(mockConnector, hc)) shouldBe Some(accountSummary)) should produce[NumberFormatException]
    }
  }
}
