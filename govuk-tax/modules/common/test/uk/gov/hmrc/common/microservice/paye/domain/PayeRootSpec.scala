package uk.gov.hmrc.common.microservice.paye.domain

import uk.gov.hmrc.common.BaseSpec
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.common.microservice.paye.PayeMicroService
import uk.gov.hmrc.microservice.txqueue.{TxQueueTransaction, TxQueueMicroService}

class PayeRootSpec extends BaseSpec with MockitoSugar {
  "The fetchTaxYearData service" should {
    "return the expected data " in {

      val benefit = mock[Benefit]
      val employment = mock[Employment]
      val tx1 = mock[TxQueueTransaction]
      val tx2 = mock[TxQueueTransaction]
      implicit val payeMicroService = mock[PayeMicroService]
      implicit val txQueueMicroService = mock[TxQueueMicroService]
      val stubPayeRoot = new PayeRoot("NM439085B", 1, "Mr", "John", None, "Densmore", "johnnyBoy", "1960-12-01", Map.empty, Map.empty, Map.empty) {
        override def fetchBenefits(taxYear: Int)(implicit payeMicroService: PayeMicroService): Seq[Benefit] = if (taxYear == 2013) Seq(benefit) else Seq.empty

        override def fetchEmployments(taxYear: Int)(implicit payeMicroService: PayeMicroService): Seq[Employment] = if (taxYear == 2013) Seq(employment) else Seq.empty

        override def fetchRecentAcceptedTransactions()(implicit txQueueMicroService: TxQueueMicroService): Seq[TxQueueTransaction] = Seq(tx1)

        override def fetchRecentCompletedTransactions()(implicit txQueueMicroService: TxQueueMicroService): Seq[TxQueueTransaction] = Seq(tx2)
      }
      stubPayeRoot.fetchTaxYearData(2013) shouldBe PayeRootData(Seq(tx1), Seq(tx2), Seq(benefit), Seq(employment))
    }
  }

}
