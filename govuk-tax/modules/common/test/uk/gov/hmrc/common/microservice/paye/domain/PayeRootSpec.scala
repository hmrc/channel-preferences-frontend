package uk.gov.hmrc.common.microservice.paye.domain

import uk.gov.hmrc.common.BaseSpec
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.common.microservice.paye.PayeConnector
import uk.gov.hmrc.common.microservice.txqueue.TxQueueConnector
import uk.gov.hmrc.common.microservice.txqueue.domain.TxQueueTransaction

class PayeRootSpec extends BaseSpec with MockitoSugar {
  "The fetchTaxYearData service" should {
    "return the expected data " in {

      val benefit = mock[Benefit]
      val employment = mock[Employment]
      val tx1 = mock[TxQueueTransaction]
      val tx2 = mock[TxQueueTransaction]
      implicit val payeConnector = mock[PayeConnector]
      implicit val txQueueConnector = mock[TxQueueConnector]
      val stubPayeRoot = new PayeRoot("NM439085B", 1, "Mr", "John", None, "Densmore", "johnnyBoy", "1960-12-01", Map.empty, Map.empty, Map.empty) {
        override def fetchBenefits(taxYear: Int)(implicit payeConnector: PayeConnector): Seq[Benefit] = if (taxYear == 2013) Seq(benefit) else Seq.empty

        override def fetchEmployments(taxYear: Int)(implicit payeConnector: PayeConnector): Seq[Employment] = if (taxYear == 2013) Seq(employment) else Seq.empty

        override def fetchRecentAcceptedTransactions()(implicit txQueueConnector: TxQueueConnector): Seq[TxQueueTransaction] = Seq(tx1)

        override def fetchRecentCompletedTransactions()(implicit txQueueConnector: TxQueueConnector): Seq[TxQueueTransaction] = Seq(tx2)
      }
      stubPayeRoot.fetchTaxYearData(2013) shouldBe PayeRootData(Seq(tx1), Seq(tx2), Seq(benefit), Seq(employment))
    }
  }

  "addBenefitLink " should {
    "look for a link called benefits " in {
      val linkMap = Map("nonsenseLink" -> "/a/nonsense/link", "benefits" -> "/addBenefit/link")
      val payeRoot = PayeRoot(nino = "NM439085B", version = 1, title = "Mr", firstName = "John", secondName = None,
        surname = "Densmore", name = "johnnyBoy", dateOfBirth = "1960-12-01", links = linkMap, Map.empty, Map.empty)
      payeRoot.addBenefitLink(2013) shouldBe Some("/addBenefit/link")
    }
  }

}
