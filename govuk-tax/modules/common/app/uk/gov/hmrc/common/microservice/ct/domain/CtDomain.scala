package ct.domain

import ct.CtMicroService
import uk.gov.hmrc.common.microservice.domain.RegimeRoot
import uk.gov.hmrc.domain.CtUtr

object CtDomain {

  case class CtRoot(links: Map[String, String]) extends RegimeRoot {

    private val accountSummaryKey = "accountSummary"

    def accountSummary(implicit ctMicroService: CtMicroService): Option[CtAccountSummary] = {
      links.get(accountSummaryKey) match {
        case Some(uri) => ctMicroService.accountSummary(uri)
        case _ => None
      }
    }
  }

  case class CtAccountSummary(accountBalance: Option[CtAccountBalance], dateOfBalance: Option[String])

  case class CtAccountBalance(amount: Option[BigDecimal], currency: Option[String])

}

