package connectors

import uk.gov.hmrc.common.microservice.MicroServiceConfig

import uk.gov.hmrc.domain.SaUtr
import scala.concurrent.Future
import uk.gov.hmrc.play.connectors.HeaderCarrier
import uk.gov.hmrc.play.logging.MdcLoggingExecutionContext._
import play.api.libs.json.Json
import play.api.http.Status
import uk.gov.hmrc.play.http.ws.WSHttp
import uk.gov.hmrc.play.http.{Upstream4xxResponse, HttpGet, HttpPost, NotFoundException}

object PreferencesConnector extends PreferencesConnector {
  override val serviceUrl = MicroServiceConfig.preferencesServiceUrl

  override def http = WSHttp
}

trait PreferencesConnector extends Status {

  def http: HttpGet with HttpPost

  def serviceUrl: String

  def url(path: String) = s"$serviceUrl$path"

  def savePreferences(utr: SaUtr, digital: Boolean, email: Option[String] = None)(implicit hc: HeaderCarrier): Future[Option[FormattedUri]] = {

    http.POST[UpdateEmail](url(s"/preferences/sa/individual/$utr/print-suppression"), UpdateEmail(digital, email)).map {
      response => Json.fromJson[FormattedUri](response.json).asOpt
    }.recover {

      case e: NotFoundException => None
    }
  }

  def getPreferences(utr: SaUtr)(implicit headerCarrier: HeaderCarrier): Future[Option[SaPreference]] = {
    http.GET[Option[SaPreference]](url(s"/preferences/sa/individual/$utr/print-suppression")).recover {
      case e: NotFoundException => None
    }
  }

  def savePreferencesUnsecured(utr: SaUtr, digital: Boolean, email: Option[String] = None)(implicit hc: HeaderCarrier): Future[Option[FormattedUri]] = {
    http.POST[SaPreferenceSimplified](url(s"/portal/preferences/sa/individual/$utr/print-suppression"), SaPreferenceSimplified(digital, email)).map {
      response => Json.fromJson[FormattedUri](response.json).asOpt
    }.recover {
      case e: NotFoundException => None
    }
  }

  def getPreferencesUnsecured(utr: SaUtr)(implicit hc: HeaderCarrier): Future[Option[SaPreference]] = {
    http.GET[Option[SaPreference]](url(s"/portal/preferences/sa/individual/$utr/print-suppression")).recover {
      case e: NotFoundException => None
    }
  }

  def updateEmailValidationStatusUnsecured(token: String)(implicit hc: HeaderCarrier): Future[EmailVerificationLinkResponse.Value] = {
    http.POST[ValidateEmail](url("/preferences/sa/verify-email"), ValidateEmail(token)).map(_ => EmailVerificationLinkResponse.OK).recover {
      case Upstream4xxResponse(_, GONE, _) => EmailVerificationLinkResponse.EXPIRED
      case _ => EmailVerificationLinkResponse.ERROR
    }
  }

}
