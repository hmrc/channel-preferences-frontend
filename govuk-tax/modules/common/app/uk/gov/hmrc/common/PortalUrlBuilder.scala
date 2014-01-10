package uk.gov.hmrc.common

import config.PortalConfig
import play.api.mvc.Request
import play.api.Logger
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.utils.TaxYearResolver
import scala.annotation.tailrec
import controllers.common.SessionKeys

trait PortalUrlBuilder {

  def buildPortalUrl(destinationPathKey: String)(implicit request: Request[AnyRef], user: User): String = {
    resolvePlaceHolder(
      url = PortalConfig.getDestinationUrl(destinationPathKey),
      tagsToBeReplacedWithData = Seq(
        ("<year>", Some(toSaTaxYearRepresentation(TaxYearResolver.currentTaxYear))),
        ("<utr>", user.userAuthority.accounts.sa.map(_.utr)),
        ("<vrn>", user.userAuthority.accounts.vat.map(_.vrn)),
        ("<ctutr>", user.userAuthority.accounts.ct.map(_.utr)),
        ("<affinitygroup>", request.session.get(SessionKeys.affinityGroup).orElse { throw new RuntimeException("Affinity Group not found") }),
        ("<empref>", user.userAuthority.accounts.epaye.map(_.empRef.value))
      ))
  }

  @tailrec
  private def resolvePlaceHolder(url: String, tagsToBeReplacedWithData: Seq[(String, Option[Any])]): String =
    if (tagsToBeReplacedWithData.isEmpty)
      url
    else
      resolvePlaceHolder(replace(url, tagsToBeReplacedWithData.head), tagsToBeReplacedWithData.tail)

  private def replace(url: String, tagToBeReplacedWithData: (String, Option[Any])): String = {
    val (tagName, tagValueOption) = tagToBeReplacedWithData
    tagValueOption match {
      case Some(valueOfTag) => url.replace(tagName, valueOfTag.toString)
      case _ => {
        if (url.contains(tagName)) {
          Logger.error(s"Failed to populate parameter $tagName in URL $url")
        }
        url
      }
    }
  }

  private[common] def toSaTaxYearRepresentation(taxYear: Int) = {
    val taxYearMinusOne = taxYear - 1
    val lastTwoDigitsThisTaxYear = toLastTwoDigitsString(taxYear)
    val lastTwoDigitsLastTaxYear = toLastTwoDigitsString(taxYearMinusOne)

    s"${lastTwoDigitsLastTaxYear}$lastTwoDigitsThisTaxYear"
  }

  def toLastTwoDigitsString(taxYear: Int): String = {
    taxYear % 100 match {
      case it if it < 10 => s"0$it"
      case it => s"$it"
    }
  }
}

