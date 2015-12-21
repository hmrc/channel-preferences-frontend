package connectors

import controllers.internal.OptInCohort
import play.api.Logger
import play.api.http.Status
import play.api.libs.json._
import uk.gov.hmrc.domain.{Nino, SaUtr}
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import uk.gov.hmrc.play.http.{NotFoundException, _}

import scala.concurrent.Future

case class Email(email: String)

object Email {
  implicit val readsEmail = Json.reads[Email]
}

sealed trait TermsType
case object Generic extends TermsType

case class TermsAccepted(accepted: Boolean)
object TermsAccepted {
  implicit val format = Json.format[TermsAccepted]
}

object PreferencesConnector extends PreferencesConnector with ServicesConfig {
  override val serviceUrl = baseUrl("preferences")

  override def http = WsHttp

  protected[connectors] case class ActivationStatus(active: Boolean)
  protected[connectors] object ActivationStatus { implicit val format = Json.format[ActivationStatus]}

  protected[connectors] case class TermsAndConditionsUpdate(generic: TermsAccepted, email: Option[String])
  protected[connectors] object TermsAndConditionsUpdate {
    implicit val format = Json.format[TermsAndConditionsUpdate]

    def from(terms: (TermsType, TermsAccepted), email: Option[String]): TermsAndConditionsUpdate = terms match {
      case (Generic, accepted: TermsAccepted) => TermsAndConditionsUpdate(generic = accepted, email = email)
      case (termsType, _) => throw new IllegalArgumentException(s"Could not work with termsType=$termsType")
    }
  }
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

  def getEmailAddress(utr: SaUtr)(implicit hc: HeaderCarrier) =
    http.GET[Option[Email]](url(s"/portal/preferences/sa/individual/$utr/print-suppression/verified-email-address"))
      .map(_.map(_.email))

  def updateEmailValidationStatusUnsecured(token: String)(implicit hc: HeaderCarrier): Future[EmailVerificationLinkResponse.Value] = {
    responseToEmailVerificationLinkStatus(http.POST(url("/preferences/sa/verify-email"), ValidateEmail(token)))
  }

  def updateTermsAndConditions(utr: SaUtr, termsAccepted: (TermsType, TermsAccepted), email: Option[String]) (implicit hc: HeaderCarrier): Future[Int] = {
    http.POST(url(s"/preferences/sa/individual/$utr/terms-and-conditions"), PreferencesConnector.TermsAndConditionsUpdate.from(termsAccepted, email)).map(_.status).recover {
      case e: Upstream4xxResponse =>
        Logger.error("Unable to save upgraded terms and conditions", e)
        e.upstreamResponseCode
      case e: Upstream5xxResponse =>
        Logger.error("Unable to save upgraded terms and conditions", e)
        e.upstreamResponseCode
    }
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