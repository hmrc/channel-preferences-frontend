package uk.gov.hmrc.common.microservice.vat.domain

import uk.gov.hmrc.common.microservice.domain.{TaxRegime, RegimeRoot}
import uk.gov.hmrc.common.microservice.vat.VatConnector
import uk.gov.hmrc.domain.{SaUtr, Vrn}
import uk.gov.hmrc.common.microservice.auth.domain.Regimes
import controllers.common.FrontEndRedirect

object VatDomain {
  object VatRegime extends TaxRegime {
    override def isAuthorised(regimes: Regimes) = {
      regimes.vat.isDefined
    }

    override def unauthorisedLandingPage: String = {
      FrontEndRedirect.businessTaxHome
    }
  }
  object VatRoot {
    def apply(vrn: Vrn, root: VatJsonRoot) = new VatRoot(vrn, root.links)
  }

  case class VatJsonRoot(links: Map[String, String])

  case class VatRoot(vrn: Vrn, links: Map[String, String]) extends RegimeRoot[Vrn] {

    private val accountSummaryKey = "accountSummary"

    override val identifier = vrn

    def accountSummary(implicit vatConnector: VatConnector): Option[VatAccountSummary] = {
      links.get(accountSummaryKey) match {
        case Some(uri) => {
          vatConnector.accountSummary(uri) match {
            case None => throw new IllegalStateException(s"Expected HOD data not found for link '$accountSummaryKey' with path: $uri")
            case summary => summary
          }
        }
        case _ => None
      }
    }
  }

  case class VatAccountSummary(accountBalance: Option[VatAccountBalance], dateOfBalance: Option[String])

  case class VatAccountBalance(amount: Option[BigDecimal])

}

