package uk.gov.hmrc.common

import config.PortalConfig
import play.api.mvc.Request
import controllers.common.CookieEncryption
import play.api.Logger
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.utils.TaxYearResolver


trait PortalDestinationUrlBuilder extends CookieEncryption {
  def buildPortalUrl(destinationPathKey: String)(implicit request: Request[AnyRef], user: User): String = {
    val currentTaxYear = TaxYearResolver.currentTaxYear
    val saUtr = user.userAuthority.saUtr
    val vrn = user.userAuthority.vrn
    val ctUtr = user.userAuthority.ctUtr
    val affinityGroup = parseOrExceptionFromSession(request, "affinityGroup")
    val destinationUrl = PortalConfig.getDestinationUrl(destinationPathKey)
    val tagsToBeReplacedWithData: Seq[(String, Option[Any])] = Seq(("<year>", Some(currentTaxYear)), ("<utr>", saUtr), ("<affinitygroup>", Some(affinityGroup)), ("<vrn>", vrn), ("<ctutr>", ctUtr))
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

