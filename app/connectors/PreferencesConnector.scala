package connectors

import controllers.sa.prefs.internal.InterstitialPageContentCohorts.Cohort
import play.api.http.Status
import play.api.libs.json._
import uk.gov.hmrc.common.microservice.MicroServiceConfig
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.play.connectors.HeaderCarrier
import uk.gov.hmrc.play.http.{Upstream4xxResponse, _}
import uk.gov.hmrc.play.http.ws.WSHttp
import uk.gov.hmrc.play.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

object PreferencesConnector extends PreferencesConnector {
  override val serviceUrl = MicroServiceConfig.preferencesServiceUrl

  override def http = WSHttp
}

trait PreferencesConnector extends Status {

  def http: HttpGet with HttpPost

  def serviceUrl: String

  def url(path: String) = s"$serviceUrl$path"

  def savePreferences(utr: SaUtr, digital: Boolean, email: Option[String], cohort: Cohort)(implicit hc: HeaderCarrier): Future[Any] =
    http.POST(url(s"/preferences/sa/individual/$utr/print-suppression"), UpdateEmail(digital, email))

  def getPreferences(utr: SaUtr)(implicit headerCarrier: HeaderCarrier): Future[Option[SaPreference]] = {
    http.GET[Option[SaPreference]](url(s"/preferences/sa/individual/$utr/print-suppression")).recover {
      case e: NotFoundException => None
    }
  }

  // TODO Could/should this use /portal/preferences/sa/individual/:utr/print-suppression/verified-email-address ?
  def getEmailAddress(utr: SaUtr)(implicit hc: HeaderCarrier) = {
    implicit val emailAddressFromPreferenceRds: Reads[Option[String]] = (__ \ "email").readNullable((__ \ "email").read[String])
    http.GET[Option[String]](url(s"/portal/preferences/sa/individual/$utr/print-suppression")).recover {
      case e: NotFoundException => None
    }
  }

  def updateEmailValidationStatusUnsecured(token: String)(implicit hc: HeaderCarrier): Future[EmailVerificationLinkResponse.Value] = {
    responseToEmailVerificationLinkStatus(http.POST[ValidateEmail](url("/preferences/sa/verify-email"), ValidateEmail(token)))
  }

  private[connectors] def responseToEmailVerificationLinkStatus(response: Future[HttpResponse])(implicit hc: HeaderCarrier) = {
    response.map(_ => EmailVerificationLinkResponse.OK)
      .recover {
      case Upstream4xxResponse(_, GONE, _) => EmailVerificationLinkResponse.EXPIRED
      case _ => EmailVerificationLinkResponse.ERROR
    }
  }

}
