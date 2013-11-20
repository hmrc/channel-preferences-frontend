package uk.gov.hmrc.common.microservice.sa.domain

import uk.gov.hmrc.common.BaseSpec
import org.mockito.Mockito._
import uk.gov.hmrc.common.microservice.sa.SaConnector
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.common.microservice.sa.domain.write.{TransactionId, SaAddressForUpdate}
import org.mockito.Matchers
import uk.gov.hmrc.domain.SaUtr
import controllers.common.actions.HeaderCarrier
import scala.concurrent.Future

class SaRootSpec extends BaseSpec with MockitoSugar {

  private val utr = SaUtr("2222233333")
  private val accountSummaryLink = s"/sa/individual/$utr/account-summary"
  private val accountSummary = SaAccountSummary(None, None, Some(1))
  implicit val hc = HeaderCarrier()

  "Requesting AccountSummary" should {

    "return an AccountSummary object if the service call is successful" in {
      val mockConnector = mock[SaConnector]
      val root = SaRoot(utr, Map("individual/account-summary" -> accountSummaryLink))
      when(mockConnector.accountSummary(accountSummaryLink)).thenReturn(Future.successful(Some(accountSummary)))
      root.accountSummary(mockConnector, hc) shouldBe Some(accountSummary)

    }

    "return None if no accountSummary link exists" in {
      val mockConnector = mock[SaConnector]
      val root = SaRoot(utr, Map[String, String]())
      root.accountSummary(mockConnector, hc) shouldBe None
      verifyZeroInteractions(mockConnector)
    }

    "throw an exception if we have an accountSummary link but it returns a not found status" in {
      val mockConnector = mock[SaConnector]
      val root = SaRoot(utr, Map("individual/account-summary" -> accountSummaryLink))
      when(mockConnector.accountSummary(accountSummaryLink)).thenReturn(Future.successful(None))

      val thrown = evaluating(root.accountSummary(mockConnector, hc) shouldBe Some(accountSummary)) should produce [IllegalStateException]

      thrown.getMessage shouldBe s"Expected HOD data not found for link 'individual/account-summary' with path: $accountSummaryLink"
    }

    "propagate an exception if thrown by the external connector while requesting the accountSummary" in {
      val mockConnector = mock[SaConnector]
      val root = SaRoot(utr, Map("individual/account-summary" -> accountSummaryLink))
      when(mockConnector.accountSummary(accountSummaryLink)).thenThrow(new NumberFormatException("Not a number"))

      evaluating(root.accountSummary(mockConnector, hc) shouldBe Some(accountSummary)) should produce [NumberFormatException]
    }
  }

  "personalDetails" should {

    "call the SA microservice when the uri is found in the SaRoot and return its value" in {
      val uri = "sa/individual/12345/personalDetails"
      val saRoot = SaRoot(SaUtr("12345"), Map("individual/details" -> uri))
      val name = SaName("Mr", "Tim", None, "Smith", None)
      val saPersonalDetails = Some(SaPerson(name, SaIndividualAddress("line1", "line2", Some("line3"), Some("line4"), Some("line5"), Some("46353"), Some("Malta"),None)))
      val saConnector = mock[SaConnector]

      when(saConnector.person(uri)).thenReturn(saPersonalDetails)

      saRoot.personalDetails(saConnector) shouldBe saPersonalDetails
      verify(saConnector).person(Matchers.eq(uri))
    }

    "return None when the personal details link is not present" in {
      val saRoot = SaRoot(SaUtr("12345"), Map[String, String]())
      val saConnector = mock[SaConnector]

      saRoot.personalDetails(saConnector) shouldBe None
      verify(saConnector, times(0)).person(Matchers.anyString())
    }

  }

  "updateIndividualMainAddress" should {

    "call the SA microservice when the uri is found in the SaRoot for updating the main address" in {
      val uri = "sa/individual/12345/mainAddress"
      val saRoot = SaRoot(SaUtr("12345"), Map("individual/details/main-address" -> uri))
      val saMainAddress = SaAddressForUpdate("line1", "line2", None, None, None, None)
      implicit val saConnector = mock[SaConnector]
      val transactionId = Right(TransactionId("12343asdfkjhaslkdfhoi3243kjh3kj4h343"))

      when(saConnector.updateMainAddress(uri, saMainAddress)).thenReturn(transactionId)

      saRoot.updateIndividualMainAddress(saMainAddress) shouldBe transactionId
      verify(saConnector).updateMainAddress(Matchers.eq(uri), Matchers.any())
    }

    "throw a IllegalStateException when link the uri is not found in the SaRoot" in {
      val saRoot = SaRoot(SaUtr("12345"), Map[String, String]())
      implicit val saConnector = mock[SaConnector]
      val saMainAddress = SaAddressForUpdate("line1", "line2", None, None, None, None)

      evaluating(saRoot.updateIndividualMainAddress(saMainAddress)) should produce[IllegalStateException]
      verify(saConnector, times(0)).accountSummary(Matchers.anyString())(hc)
    }
  }
}
