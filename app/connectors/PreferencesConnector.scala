package connectors


import controllers.sa.prefs.internal.Cohort
import play.api.Logger
import play.api.http.Status
import play.api.libs.json._
import uk.gov.hmrc.play.microservice.MicroServiceConfig
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.http.NotFoundException
import uk.gov.hmrc.play.http._
import uk.gov.hmrc.play.http.ws.WSHttp
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

object PreferencesConnector extends PreferencesConnector {
  override val serviceUrl = MicroServiceConfig.preferencesServiceUrl

  override def http = WSHttp
}

trait PreferencesConnector extends Status {

  def http: HttpGet with HttpPost with HttpPut

  def serviceUrl: String

  def url(path: String) = s"$serviceUrl$path"

  def savePreferences(utr: SaUtr, digital: Boolean, email: Option[String])(implicit hc: HeaderCarrier): Future[Any] =
    http.POST(url(s"/preferences/sa/individual/$utr/print-suppression"), UpdateEmail(digital, email))

  def getPreferences(utr: SaUtr)(implicit headerCarrier: HeaderCarrier): Future[Option[SaPreference]] = {
    http.GET[Option[SaPreference]](url(s"/preferences/sa/individual/$utr/print-suppression")).recover {
      case response: Upstream4xxResponse if response.upstreamResponseCode == GONE => None
      case e: NotFoundException => None
    }
  }

  def saveCohort(utr: SaUtr, cohort: Cohort)(implicit hc: HeaderCarrier): Future[Any] = {

    http.PUT(url(s"/a-b-testing/cohort/email-opt-in/sa/$utr"), Json.obj("cohort" -> cohort.name)).recover {
      case e: NotFoundException => Logger.warn("Cannot save cohort for opt-in-email")
    }
  }

  // TODO Could/should this use /portal/preferences/sa/individual/:utr/print-suppression/verified-email-address ?
  def getEmailAddress(utr: SaUtr)(implicit hc: HeaderCarrier) = {
    implicit val emailAddressFromPreferenceRds: Reads[Option[String]] = (__ \ "email").readNullable((__ \ "email").read[String])
    http.GET[Option[String]](url(s"/portal/preferences/sa/individual/$utr/print-suppression")).recover {
      case response: Upstream4xxResponse if response.upstreamResponseCode == GONE => None
      case e: NotFoundException => None
    }
  }

  def updateEmailValidationStatusUnsecured(token: String)(implicit hc: HeaderCarrier): Future[EmailVerificationLinkResponse.Value] = {
    responseToEmailVerificationLinkStatus(http.POST[ValidateEmail](url("/preferences/sa/verify-email"), ValidateEmail(token)))
  }

  private[connectors] def responseToEmailVerificationLinkStatus(response: Future[HttpResponse])(implicit hc: HeaderCarrier) = {
    response.map(_ => EmailVerificationLinkResponse.Ok)
      .recover {
      case Upstream4xxResponse(_, GONE, _) => EmailVerificationLinkResponse.Expired
      case Upstream4xxResponse(_, CONFLICT, _) => EmailVerificationLinkResponse.WrongToken
      case (_:Upstream4xxResponse |_: NotFoundException |_:BadRequestException) => EmailVerificationLinkResponse.Error
    }
  }

}
