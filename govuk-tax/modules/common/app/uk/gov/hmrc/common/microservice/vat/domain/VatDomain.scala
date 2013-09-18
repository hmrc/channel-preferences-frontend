package uk.gov.hmrc.common.microservice.vat.domain

import uk.gov.hmrc.common.microservice.domain.RegimeRoot
import uk.gov.hmrc.common.microservice.vat.VatMicroService
import uk.gov.hmrc.common.microservice.auth.domain.Vrn

object VatDomain {

  case class VatRoot(vrn: Vrn, links: Map[String, String]) extends RegimeRoot {

    private val accountSummaryKey = "accountSummary"

    def accountSummary(implicit vatMicroService: VatMicroService): Option[VatAccountSummary] = {
      links.get(accountSummaryKey) match {
        case Some(uri) => vatMicroService.accountSummary(uri)
        case _ => None
      }
    }
  }

  case class VatAccountSummary(accountBalance: Option[VatAccountBalance], dateOfBalance: Option[String])

  case class VatAccountBalance(amount: Option[BigDecimal], currency: Option[String])

}

