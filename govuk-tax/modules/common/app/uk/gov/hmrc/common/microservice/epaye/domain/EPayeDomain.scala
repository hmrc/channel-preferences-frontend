package uk.gov.hmrc.common.microservice.epaye.domain

import uk.gov.hmrc.common.microservice.domain.RegimeRoot
import uk.gov.hmrc.common.microservice.epaye.EPayeConnector
import uk.gov.hmrc.domain.EmpRef

object EPayeDomain {

  case class EPayeLinks(accountSummary: Option[String])

  case class EPayeRoot(links: EPayeLinks) extends RegimeRoot {

    def accountSummary(implicit epayeConnector: EPayeConnector): Option[EPayeAccountSummary] = {
      links.accountSummary match {
        case Some(accountSummaryPath) => epayeConnector.accountSummary(accountSummaryPath)
        case _ => None
      }
    }
  }

  case class EPayeAccountSummary(rti: Option[RTI] = None, nonRti: Option[NonRTI] = None)
  case class RTI(balance: BigDecimal)
  case class NonRTI(paidToDate: BigDecimal, currentTaxYear: Int)
}
