package uk.gov.hmrc.common.microservice.epaye.domain

import uk.gov.hmrc.common.microservice.domain.RegimeRoot
import uk.gov.hmrc.common.microservice.epaye.EPayeMicroService
import uk.gov.hmrc.domain.EmpRef

object EPayeDomain {

  case class EPayeRoot(empRef: EmpRef, links: Map[String, String]) extends RegimeRoot {

    private val accountSummaryKey = "accountSummary"

    def accountSummary(implicit ePayeMicroService: EPayeMicroService): Option[EPayeAccountSummary] = {
      links.get(accountSummaryKey) match {
        case Some(uri) => ePayeMicroService.accountSummary(uri)
        case _ => None
      }
    }
  }

  case class EPayeAccountSummary(rti: Option[RTI] = None, nonRti: Option[NonRTI] = None)
  case class RTI(balance: BigDecimal)
  case class NonRTI(paidToDate: AmountDue, currentTaxYear: Int)

  case class AmountDue(amount: BigDecimal)
}
