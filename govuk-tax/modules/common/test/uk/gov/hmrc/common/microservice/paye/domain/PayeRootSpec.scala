package uk.gov.hmrc.common.microservice.paye.domain

import uk.gov.hmrc.common.BaseSpec
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.common.microservice.paye.PayeConnector
import uk.gov.hmrc.common.microservice.txqueue.TxQueueConnector
import controllers.common.actions.HeaderCarrier
import scala.concurrent.Future
import org.scalatest.concurrent.ScalaFutures
import org.mockito.Mockito._
import org.joda.time.{LocalDate, DateTimeZone, DateTime}
import uk.gov.hmrc.common.microservice.txqueue.domain.TxQueueTransaction
import java.net.URI
import uk.gov.hmrc.utils.DateTimeUtils

class PayeRootSpec extends BaseSpec with MockitoSugar with ScalaFutures {
  "The fetchTaxYearData service" should {
    "return the expected data " in {

      implicit val hc = HeaderCarrier()

      val carBenefit = CarBenefit(2013, 1, new LocalDate, new LocalDate, 0.0,0.0,"Diesel", Some(1400),Some(125),3000,0,0,new LocalDate,None, None)
      val employment = mock[Employment]
      implicit val payeConnector = mock[PayeConnector]
      implicit val txQueueConnector = mock[TxQueueConnector]

      val stubPayeRoot = new PayeRoot("NM439085B", 1, "Mr", "John", None, "Densmore", "johnnyBoy", "1960-12-01", Map.empty, Map.empty, Map.empty) {
        override def fetchCars(taxYear: Int)(implicit payeConnector: PayeConnector, headerCarrier: HeaderCarrier): Future[Seq[CarBenefit]] =
          Future.successful(if (taxYear == 2013) Seq(carBenefit) else Seq.empty)

        override def fetchEmployments(taxYear: Int)(implicit payeConnector: PayeConnector, headerCarrier: HeaderCarrier): Future[Seq[Employment]] =
          Future.successful(if (taxYear == 2013) Seq(employment) else Seq.empty)

      }

      val expectedTaxYearData: TaxYearData = TaxYearData(Seq(carBenefit), Seq(employment))

      whenReady(stubPayeRoot.fetchTaxYearData(2013)) { taxYearData =>
        taxYearData shouldBe expectedTaxYearData
      }
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

  "The fetchTransactionHistory service" should {

    "make only one call to txQueue if there are at least three transactions in the last 30 days" in {
      val nino = "NM439085B"
      val linkMap = Map("history" -> s"/txqueue/current-status/paye/$nino/history/after/{from}?statuses={statuses}&max-results={maxResults}")
      val payeRoot = PayeRoot(nino = nino, version = 1, title = "Mr", firstName = "John", secondName = None,
        surname = "Densmore", name = "johnnyBoy", dateOfBirth = "1960-12-01", transactionLinks = linkMap, links = Map.empty, actions =  Map.empty)

      val txHistory = Some(List(
        TxQueueTransaction(id =  new URI("uri1"), user =  new URI("uri"), callback = None, regime = "paye", statusHistory = List.empty, properties = Map.empty, createdAt = DateTimeUtils.now, lastUpdatedAt = DateTimeUtils.now),
        TxQueueTransaction(id =  new URI("uri2"), user =  new URI("uri"), callback = None, regime = "paye", statusHistory = List.empty, properties = Map.empty, createdAt = DateTimeUtils.now, lastUpdatedAt = DateTimeUtils.now),
        TxQueueTransaction(id =  new URI("uri3"), user =  new URI("uri"), callback = None, regime = "paye", statusHistory = List.empty, properties = Map.empty, createdAt = DateTimeUtils.now, lastUpdatedAt = DateTimeUtils.now),
        TxQueueTransaction(id =  new URI("uri4"), user =  new URI("uri"), callback = None, regime = "paye", statusHistory = List.empty, properties = Map.empty, createdAt = DateTimeUtils.now, lastUpdatedAt = DateTimeUtils.now)
      ))
      implicit val hc = HeaderCarrier()
      val txQueueConnector = mock[TxQueueConnector]
      when(txQueueConnector.transaction(s"/txqueue/current-status/paye/$nino/history/after/2013-11-10?statuses=ACCEPTED,COMPLETED&max-results=0"))
        .thenReturn(Future.successful(txHistory))

      val result = payeRoot.fetchTransactionHistory(txQueueConnector, () => new DateTime(2013, 12, 10, 0, 0, DateTimeZone.UTC))
      whenReady(result) {
        actualTxHistory =>
          verify(txQueueConnector).transaction(s"/txqueue/current-status/paye/$nino/history/after/2013-11-10?statuses=ACCEPTED,COMPLETED&max-results=0")
          verifyNoMoreInteractions(txQueueConnector)
          Some(actualTxHistory) shouldBe txHistory
      }
    }

    "make two calls to the txQueue if there are less than three transactions in the last 30 days" in {
      val nino = "NM439085B"
      val linkMap = Map("history" -> s"/txqueue/current-status/paye/$nino/history/after/{from}?statuses={statuses}&max-results={maxResults}")
      val payeRoot = PayeRoot(nino = nino, version = 1, title = "Mr", firstName = "John", secondName = None,
        surname = "Densmore", name = "johnnyBoy", dateOfBirth = "1960-12-01", transactionLinks = linkMap, links = Map.empty, actions =  Map.empty)

      val firstTxHistory = Some(List(
        TxQueueTransaction(id =  new URI("uri1"), user =  new URI("uri"), callback = None, regime = "paye", statusHistory = List.empty, properties = Map.empty, createdAt = DateTimeUtils.now, lastUpdatedAt = DateTimeUtils.now),
        TxQueueTransaction(id =  new URI("uri2"), user =  new URI("uri"), callback = None, regime = "paye", statusHistory = List.empty, properties = Map.empty, createdAt = DateTimeUtils.now, lastUpdatedAt = DateTimeUtils.now)
      ))

      val secondTxHistory = Some(firstTxHistory.get ++ List(
        TxQueueTransaction(id =  new URI("uri3"), user =  new URI("uri"), callback = None, regime = "paye", statusHistory = List.empty, properties = Map.empty, createdAt = DateTimeUtils.now, lastUpdatedAt = DateTimeUtils.now)
      ))

      implicit val hc = HeaderCarrier()
      val txQueueConnector = mock[TxQueueConnector]
      when(txQueueConnector.transaction(s"/txqueue/current-status/paye/$nino/history/after/2013-11-10?statuses=ACCEPTED,COMPLETED&max-results=0"))
        .thenReturn(Future.successful(firstTxHistory))
      when(txQueueConnector.transaction(s"/txqueue/current-status/paye/$nino/history/after/1970-01-01?statuses=ACCEPTED,COMPLETED&max-results=3"))
        .thenReturn(Future.successful(secondTxHistory))

      val result = payeRoot.fetchTransactionHistory(txQueueConnector, () => new DateTime(2013, 12, 10, 0, 0, DateTimeZone.UTC))
      whenReady(result) {
        actualTxHistory =>
          verify(txQueueConnector).transaction(s"/txqueue/current-status/paye/$nino/history/after/2013-11-10?statuses=ACCEPTED,COMPLETED&max-results=0")
          verify(txQueueConnector).transaction(s"/txqueue/current-status/paye/$nino/history/after/1970-01-01?statuses=ACCEPTED,COMPLETED&max-results=3")
          verifyNoMoreInteractions(txQueueConnector)
          Some(actualTxHistory) shouldBe secondTxHistory
      }
    }
  }

}
