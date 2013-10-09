package uk.gov.hmrc.common.microservice.ct.domain

import uk.gov.hmrc.common.microservice.domain.RegimeRoot
import uk.gov.hmrc.common.microservice.ct.CtConnector
import uk.gov.hmrc.domain.CtUtr

object CtDomain {

  object CtRoot {
    def apply(utr: CtUtr, root: CtJsonRoot): CtRoot = new CtRoot(utr, root.links)
  }

  case class CtRoot(utr: CtUtr, links: Map[String, String]) extends RegimeRoot[CtUtr] {

    private val accountSummaryKey = "accountSummary"

    override val identifier = utr

    def accountSummary(implicit ctConnector: CtConnector): Option[CtAccountSummary] = {
      links.get(accountSummaryKey) match {
        case Some(uri) => ctConnector.accountSummary(uri)
        case _ => None
      }
    }
  }

  case class CtAccountSummary(accountBalance: Option[CtAccountBalance], dateOfBalance: Option[String])

  case class CtAccountBalance(amount: Option[BigDecimal], currency: Option[String])

  case class CtJsonRoot(links: Map[String, String])
}

