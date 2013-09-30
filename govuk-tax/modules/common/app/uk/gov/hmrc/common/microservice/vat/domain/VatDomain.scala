package uk.gov.hmrc.common.microservice.vat.domain

import uk.gov.hmrc.common.microservice.domain.RegimeRoot
import uk.gov.hmrc.common.microservice.vat.VatConnector
import uk.gov.hmrc.domain.Vrn

object VatDomain {

  case class VatRoot(vrn: Vrn, links: Map[String, String]) extends RegimeRoot[Vrn] {

    private val accountSummaryKey = "accountSummary"

    def identifier = vrn

    def accountSummary(implicit vatConnector: VatConnector): Option[VatAccountSummary] = {
      links.get(accountSummaryKey) match {
        case Some(uri) => vatConnector.accountSummary(uri)
        case _ => None
      }
    }
  }

  case class VatAccountSummary(accountBalance: Option[VatAccountBalance], dateOfBalance: Option[String])

  case class VatAccountBalance(amount: Option[BigDecimal], currency: Option[String])

}

