package controllers.paye

import uk.gov.hmrc.microservice.txqueue.TxQueueTransaction
import uk.gov.hmrc.common.microservice.paye.domain.{Employment, Benefit}


case class PayeRootData(acceptedTransactions: Seq[TxQueueTransaction], completedTransactions: Seq[TxQueueTransaction],
                        currentTaxYearBenefits: Seq[Benefit], currentTaxYearEmployments: Seq[Employment])
