package uk.gov.hmrc.common

import uk.gov.hmrc.microservice.domain.User
import config.PortalConfig
import org.joda.time.LocalDate
import uk.gov.hmrc.microservice.auth.domain.Utr

object PortalDestinationUrlBuilder {
  def buildUrl(destinationPathKey: String, user: User): String = {
    buildUrl(destinationPathKey, user.userAuthority.utr)
  }

  def buildUrl(destinationPathKey: String, utr: Option[Utr]): String = {
    val destinationUrl = PortalConfig.getDestinationUrl(destinationPathKey)
    val currentTaxYear = TaxYearResolver.currentTaxYear(new LocalDate)

    utr match {
      case Some(utrValue) => destinationUrl.replace("<utr>", utrValue.utr).replace("<year>", s"$currentTaxYear")
      case _ => destinationUrl.replace("<year>", s"$currentTaxYear")
    }
  }
}
