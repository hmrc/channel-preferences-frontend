package uk.gov.hmrc.common.microservice.ct.domain

import uk.gov.hmrc.common.microservice.domain.RegimeRoot
import uk.gov.hmrc.common.microservice.ct.CtConnector
import uk.gov.hmrc.domain.CtUtr

object CtDomain {

  case class CtRoot(links: Map[String, String], ctUtr: CtUtr) extends RegimeRoot[CtUtr] {

    private val accountSummaryKey = "accountSummary"

    def identifier = ctUtr

    def accountSummary(implicit ctConnector: CtConnector): Option[CtAccountSummary] = {
      links.get(accountSummaryKey) match {
        case Some(uri) => ctConnector.accountSummary(uri)
        case _ => None
      }
    }
  }

  case class CtAccountSummary(accountBalance: Option[CtAccountBalance], dateOfBalance: Option[String])

  case class CtAccountBalance(amount: Option[BigDecimal], currency: Option[String])

}

