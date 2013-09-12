package uk.gov.hmrc.common

import config.PortalConfig
import org.joda.time.LocalDate
import play.api.mvc.Request
import scala._
import uk.gov.hmrc.microservice.domain.User
import scala.AnyRef
import scala.Some
import controllers.common.CookieEncryption
import play.api.Logger

object PortalDestinationUrlBuilder extends CookieEncryption {

  def build(request: Request[AnyRef], user: User)(destinationPathKey: String): String = {
    val currentTaxYear = TaxYearResolver.currentTaxYear(new LocalDate)
    val utr = user.userAuthority.utr
    val vrn = user.userAuthority.vrn
    val affinityGroup = parseOrExceptionFromSession(request, "affinityGroup")
    val destinationUrl = PortalConfig.getDestinationUrl(destinationPathKey)
    val tagsToBeReplacedWithData: Seq[(String, Option[Any])] = Seq(("<year>", Some(currentTaxYear)), ("<utr>", utr), ("<affinitygroup>", Some(affinityGroup)), ("<vrn>", vrn))
    resolvePlaceHolder(destinationUrl, tagsToBeReplacedWithData)
  }

  private def parseOrExceptionFromSession(request: Request[AnyRef], key: String): String = {
    request.session.get(key) match {
      case Some(value) => decrypt(value)
      case _ => throw new RuntimeException(s"Required value for [$key] is missing in the session")
    }
  }

  private def resolvePlaceHolder(url: String, tagsToBeReplacedWithData: Seq[(String, Option[Any])]): String = {
    if (tagsToBeReplacedWithData.isEmpty) url else resolvePlaceHolder(replace(url, tagsToBeReplacedWithData.head), tagsToBeReplacedWithData.tail)
  }

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
}

