package uk.gov.hmrc.common.microservice.epaye.domain

import uk.gov.hmrc.common.microservice.domain.RegimeRoot
import uk.gov.hmrc.common.microservice.epaye.EPayeConnector
import uk.gov.hmrc.domain.EmpRef

object EPayeDomain {

  case class EPayeLinks(accountSummary: Option[String])

  case class EPayeRoot(links: EPayeLinks, empref: EmpRef) extends RegimeRoot[EmpRef] {
    def identifier = empref

    def accountSummary(implicit epayeConnector: EPayeConnector): Option[EPayeAccountSummary] = {
      links.accountSummary match {
        case Some(accountSummaryPath) => epayeConnector.accountSummary(accountSummaryPath)
        case _ => None
      }
    }
  }

  object EPayeRoot {
    def apply(root : EPayeJsonRoot, empref: EmpRef) : EPayeRoot = new EPayeRoot(root.links, empref)
  }

  case class EPayeJsonRoot(links: EPayeLinks)

  case class EPayeAccountSummary(rti: Option[RTI] = None, nonRti: Option[NonRTI] = None)

  case class RTI(balance: BigDecimal)

  case class NonRTI(paidToDate: BigDecimal, currentTaxYear: Int)

}
