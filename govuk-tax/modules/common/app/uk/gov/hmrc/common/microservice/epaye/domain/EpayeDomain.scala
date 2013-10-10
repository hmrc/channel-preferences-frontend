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
        case Some(uri) => {
          epayeConnector.accountSummary(uri) match {
            case None => throw new IllegalStateException(s"Expected HOD data not found for link 'accountSummary' with path: $uri")
            case summary => summary
          }
        }
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
