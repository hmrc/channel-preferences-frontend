package connectors


import controllers.sa.prefs.internal.OptInCohort
import play.api.Logger
import play.api.http.Status
import play.api.libs.json._
import uk.gov.hmrc.domain.{Nino, SaUtr}
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import uk.gov.hmrc.play.http.{NotFoundException, _}

import scala.concurrent.Future

object PreferencesConnector extends PreferencesConnector with ServicesConfig {
  override val serviceUrl = baseUrl("preferences")

  override def http = HttpVerbs
}

case class Email(email: String)

object Email {
  implicit val readsEmail = Json.reads[Email]
}

trait PreferencesConnector extends Status {

  def http: HttpGet with HttpPost with HttpPut

  def serviceUrl: String

  def url(path: String) = s"$serviceUrl$path"

  def savePreferences(utr: SaUtr, digital: Boolean, email: Option[String])(implicit hc: HeaderCarrier): Future[Any] =
    http.POST(url(s"/preferences/sa/individual/$utr/print-suppression"), UpdateEmail(digital, email))

  def getPreferences(utr: SaUtr, nino: Option[Nino] = None)(implicit headerCarrier: HeaderCarrier): Future[Option[SaPreference]] = {
    val preferencesUrl = nino.fold(s"/preferences/sa/individual/$utr/print-suppression")(n => s"/preferences/sa/individual/$utr/$n/print-suppression")
    http.GET[Option[SaPreference]](url(preferencesUrl)).recover {
      case response: Upstream4xxResponse if response.upstreamResponseCode == GONE => None
      case e: NotFoundException => None
    }
  }

  def saveCohort(utr: SaUtr, cohort: OptInCohort)(implicit hc: HeaderCarrier): Future[Any] = {

    http.PUT(url(s"/a-b-testing/cohort/email-opt-in/sa/$utr"), Json.obj("cohort" -> cohort.name)).recover {
      case e: NotFoundException => Logger.warn("Cannot save cohort for opt-in-email")
    }
  }

  def getEmailAddress(utr: SaUtr)(implicit hc: HeaderCarrier) =
    http.GET[Option[Email]](url(s"/portal/preferences/sa/individual/$utr/print-suppression/verified-email-address"))
      .map(_.map(_.email))

  def updateEmailValidationStatusUnsecured(token: String)(implicit hc: HeaderCarrier): Future[EmailVerificationLinkResponse.Value] = {
    responseToEmailVerificationLinkStatus(http.POST(url("/preferences/sa/verify-email"), ValidateEmail(token)))
  }

  private[connectors] def responseToEmailVerificationLinkStatus(response: Future[HttpResponse])(implicit hc: HeaderCarrier) = {
    response.map(_ => EmailVerificationLinkResponse.Ok)
      .recover {
      case Upstream4xxResponse(_, GONE, _, _) => EmailVerificationLinkResponse.Expired
      case Upstream4xxResponse(_, CONFLICT, _, _) => EmailVerificationLinkResponse.WrongToken
      case (_: Upstream4xxResponse | _: NotFoundException | _: BadRequestException) => EmailVerificationLinkResponse.Error
    }
  }

}
