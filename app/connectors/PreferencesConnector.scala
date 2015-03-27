package connectors


import controllers.sa.prefs.internal.OptInCohort
import play.api.Logger
import play.api.http.Status
import play.api.libs.json._
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.config.{ServicesConfig, WSHttp}
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import uk.gov.hmrc.play.http.{NotFoundException, _}

import scala.concurrent.Future

object PreferencesConnector extends PreferencesConnector with ServicesConfig {
  override val serviceUrl = baseUrl("preferences")

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

  def saveCohort(utr: SaUtr, cohort: OptInCohort)(implicit hc: HeaderCarrier): Future[Any] = {

    http.PUT(url(s"/a-b-testing/cohort/email-opt-in/sa/$utr"), Json.obj("cohort" -> cohort.name)).recover {
      case e: NotFoundException => Logger.warn("Cannot save cohort for opt-in-email")
    }
  }

  def getEmailAddress(utr: SaUtr)(implicit hc: HeaderCarrier) = {
    implicit val rds: Reads[Option[String]] = (__ \ "email").readNullable((__ \ "email").read[String])

    implicit val readOptionOf: HttpReads[Option[String]] = new HttpReads[Option[String]] {
      def read(method: String, url: String, response: HttpResponse) = response.status match {
        case 204 | 404 | 410 => None
        case _ => HttpReads.readFromJson[Option[String]].read(method, url, response)
      }
    }

    http.GET[Option[String]](url(s"/portal/preferences/sa/individual/$utr/print-suppression"))
  }

  def updateEmailValidationStatusUnsecured(token: String)(implicit hc: HeaderCarrier): Future[EmailVerificationLinkResponse.Value] = {
    responseToEmailVerificationLinkStatus(http.POST[ValidateEmail](url("/preferences/sa/verify-email"), ValidateEmail(token)))
  }

  private[connectors] def responseToEmailVerificationLinkStatus(response: Future[HttpResponse])(implicit hc: HeaderCarrier) = {
    response.map(_ => EmailVerificationLinkResponse.Ok)
      .recover {
      case Upstream4xxResponse(_, GONE, _, _) => EmailVerificationLinkResponse.Expired
      case Upstream4xxResponse(_, CONFLICT, _, _) => EmailVerificationLinkResponse.WrongToken
      case (_:Upstream4xxResponse |_: NotFoundException |_:BadRequestException) => EmailVerificationLinkResponse.Error
    }
  }

}
