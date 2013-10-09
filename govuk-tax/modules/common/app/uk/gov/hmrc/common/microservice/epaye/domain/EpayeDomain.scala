package uk.gov.hmrc.common.microservice.epaye.domain

import uk.gov.hmrc.common.microservice.domain.RegimeRoot
import uk.gov.hmrc.common.microservice.epaye.EpayeConnector
import uk.gov.hmrc.domain.EmpRef

object EpayeDomain {

  case class EpayeLinks(accountSummary: Option[String])

  case class EpayeRoot(empRef: EmpRef, links: EpayeLinks) extends RegimeRoot[EmpRef] {

    override val identifier = empRef

    def accountSummary(implicit epayeConnector: EpayeConnector): Option[EpayeAccountSummary] = {
      links.accountSummary match {
        case Some(accountSummaryPath) => epayeConnector.accountSummary(accountSummaryPath)
        case _ => None
      }
    }
  }

  object EpayeRoot {
    def apply(empRef: EmpRef, root : EpayeJsonRoot) : EpayeRoot = new EpayeRoot(empRef, root.links)
  }

  case class EpayeJsonRoot(links: EpayeLinks)

  case class EpayeAccountSummary(rti: Option[RTI] = None, nonRti: Option[NonRTI] = None)

  case class RTI(balance: BigDecimal)

  case class NonRTI(paidToDate: BigDecimal, currentTaxYear: Int)

}
