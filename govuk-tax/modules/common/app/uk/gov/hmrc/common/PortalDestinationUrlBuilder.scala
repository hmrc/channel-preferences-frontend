package uk.gov.hmrc.common

import config.PortalConfig
import org.joda.time.LocalDate
import play.api.mvc.Request
import scala._
import uk.gov.hmrc.microservice.domain.User
import scala.AnyRef
import scala.Some
import controllers.common.CookieEncryption

object PortalDestinationUrlBuilder extends CookieEncryption {

  def build(request: Request[AnyRef], user: User)(destinationPathKey: String): String = {
    val currentTaxYear = TaxYearResolver.currentTaxYear(new LocalDate)
    val utr = user.userAuthority.utr
    val vrn = user.userAuthority.vrn
    val affinityGroup = parseOrExceptionFromSession(request, "affinityGroup")
    val destinationUrl = PortalConfig.getDestinationUrl(destinationPathKey)
    val userData: Seq[(String, Option[Any])] = Seq(("<year>", Some(currentTaxYear)), ("<utr>", utr), ("<affinitygroup>", Some(affinityGroup)), ("<vrn>", vrn))
    resolvePlaceHolder(destinationUrl, userData)
  }

  private def parseOrExceptionFromSession(request: Request[AnyRef], key: String): String = {
    request.session.get(key) match {
      case Some(value) => decrypt(value)
      case _ => throw new RuntimeException(s"Required value for [$key] is missing in the session")
    }
  }

  private def resolvePlaceHolder(url: String, values: Seq[(String, Option[Any])]): String = {
    if (values.isEmpty) url else resolvePlaceHolder(replace(url, values.head), values.tail)
  }

  private def replace(url: String, values: (String, Option[Any])): String = {
    values._2 match {
      case Some(value) => url.replace(values._1, value.toString)
      case _ => url
    }
  }
}