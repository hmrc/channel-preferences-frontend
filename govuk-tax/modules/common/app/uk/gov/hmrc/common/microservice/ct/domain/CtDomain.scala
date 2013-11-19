package uk.gov.hmrc.common.microservice.ct.domain

import uk.gov.hmrc.common.microservice.domain.{RegimeRoot, TaxRegime}
import uk.gov.hmrc.common.microservice.auth.domain.Regimes
import controllers.common.{GovernmentGateway, FrontEndRedirect}
import uk.gov.hmrc.domain.CtUtr
import uk.gov.hmrc.common.microservice.ct.CtConnector
import org.joda.time.LocalDate
import java.text.SimpleDateFormat
import org.joda.time.format.DateTimeFormat

object CtRegime extends TaxRegime {
  def isAuthorised(regimes: Regimes) = regimes.ct.isDefined

  def unauthorisedLandingPage = FrontEndRedirect.businessTaxHome

  def authenticationType = GovernmentGateway
}

object CtRoot {
  def apply(utr: CtUtr, root: CtJsonRoot): CtRoot = new CtRoot(utr, root.links)
}

case class CtJsonRoot(links: Map[String, String])

case class CtRoot(utr: CtUtr, links: Map[String, String]) extends RegimeRoot[CtUtr] {

  private val accountSummaryKey = "accountSummary"

  override val identifier = utr

  def accountSummary(implicit ctConnector: CtConnector) = {
    links.get(accountSummaryKey) match {
      case Some(uri) => {
        ctConnector.accountSummary(uri) match {
          case None => throw new IllegalStateException(s"Expected HOD data not found for link '$accountSummaryKey' with path: $uri")
          case summary => summary
        }
      }
      case _ => None
    }
  }
}

case class CtAccountSummary(accountBalance: Option[CtAccountBalance], dateOfBalance: Option[String])

case class CtAccountBalance(amount: Option[BigDecimal])