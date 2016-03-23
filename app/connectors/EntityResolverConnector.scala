package connectors

import java.net.URLEncoder

import config.ServicesCircuitBreaker
import controllers.internal.OptInCohort
import model.{FormType, HostContext, NoticeOfCoding, SaAll}
import play.api.Logger
import play.api.http.Status
import play.api.libs.json._
import uk.gov.hmrc.circuitbreaker.UnhealthyServiceException
import uk.gov.hmrc.domain.{Nino, TaxIdentifier, SaUtr}
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

sealed trait PreferencesStatus

case object PreferencesExists extends PreferencesStatus

case object PreferencesCreated extends PreferencesStatus

case object PreferencesFailure extends PreferencesStatus

case class TermsAccepted(accepted: Boolean)

object TermsAccepted {
  implicit val format = Json.format[TermsAccepted]
}

case class ActivationResponse(status: Int, body: String)

object ActivationResponse {
  implicit val reads: HttpReads[ActivationResponse] = new HttpReads[ActivationResponse] with HttpErrorFunctions {
    def read(method: String, url: String, response: HttpResponse) = response.status match {
      case status if status < 500 => ActivationResponse(response.status, response.body)
      case status if is5xx(status) => throw new Upstream5xxResponse(upstreamResponseMessage(method, url, status, response.body), status, 502)
      case status => throw new Exception(s"$method to $url failed with status $status. Response body: '${response.body}'")
    }
  }
}

object EntityResolverConnector extends EntityResolverConnector with ServicesConfig {
  override val serviceUrl = baseUrl("entity-resolver")

  override def http = WsHttp

  protected[connectors] case class ActivationStatus(active: Boolean)

  protected[connectors] object ActivationStatus {
    implicit val format = Json.format[ActivationStatus]
  }

  protected[connectors] case class TermsAndConditionsUpdate(generic: TermsAccepted, email: Option[String])

  protected[connectors] object TermsAndConditionsUpdate {
    implicit val format = Json.format[TermsAndConditionsUpdate]

    def from(terms: (TermsType, TermsAccepted), email: Option[String]): TermsAndConditionsUpdate = terms match {
      case (Generic, accepted: TermsAccepted) => TermsAndConditionsUpdate(generic = accepted, email = email)
      case (termsType, _) => throw new IllegalArgumentException(s"Could not work with termsType=$termsType")
    }
  }

}

trait EntityResolverConnector extends Status with ServicesCircuitBreaker {
  this: ServicesConfig =>

  val externalServiceName = "entity-resolver"

  def http: HttpGet with HttpPost with HttpPut

  def serviceUrl: String

  def url(path: String) = s"$serviceUrl$path"

  def activate(formType: FormType,
               taxIdentifier: String,
               hostContext: HostContext,
               payload: JsValue)
              (implicit hc: HeaderCarrier): Future[ActivationResponse] = {
    def urlEncode(text: String) = URLEncoder.encode(text, "UTF-8")

    def activationUrl(formType: FormType) = {
      val hostContextQueryParams = s"returnUrl=${urlEncode(hostContext.returnUrl)}&returnLinkText=${urlEncode(hostContext.returnLinkText)}"
      formType match {
        case SaAll => url(s"/preferences/sa/individual/$taxIdentifier/activations/${formType.value}?$hostContextQueryParams")
        case NoticeOfCoding => url(s"/preferences/paye/individual/$taxIdentifier/activations/${formType.value}?$hostContextQueryParams")
      }
    }

    withCircuitBreaker {
      http.PUT[JsValue, ActivationResponse](
        url = activationUrl(formType),
        body = payload
      )
    }
  }

  def savePreferences(taxIdentifier: TaxIdentifier, digital: Boolean, email: Option[String])(implicit hc: HeaderCarrier): Future[Any] =
    withCircuitBreaker(http.POST(url(s"/preferences/$taxIdentifier"), UpdateEmail(digital, email)))

  def getPreferences(taxIdentifier: TaxIdentifier)(implicit headerCarrier: HeaderCarrier): Future[Option[SaPreference]] =
    withCircuitBreaker(http.GET[Option[SaPreference]](url(s"/preferences/$taxIdentifier")))
      .recover {
        case response: Upstream4xxResponse if response.upstreamResponseCode == GONE => None
        case e: NotFoundException => None
      }

  def saveCohort(utr: SaUtr, cohort: OptInCohort)(implicit hc: HeaderCarrier): Future[Any] = Future.successful(true)

  def getEmailAddress(utr: SaUtr)(implicit hc: HeaderCarrier) =
    withCircuitBreaker(http.GET[Option[Email]](url(s"/portal/preferences/sa/individual/$utr/print-suppression/verified-email-address")))
      .map(_.map(_.email))

  def getEmailAddress(nino: Nino)(implicit hc: HeaderCarrier) =
    withCircuitBreaker(http.GET[Option[Email]](url(s"/portal/preferences/paye/individual/$nino/print-suppression/verified-email-address")))
      .map(_.map(_.email))

  def updateEmailValidationStatusUnsecured(token: String)(implicit hc: HeaderCarrier): Future[EmailVerificationLinkResponse.Value] = {
    responseToEmailVerificationLinkStatus(withCircuitBreaker(http.PUT(url("/portal/preferences/email"), ValidateEmail(token))))
  }

  def updateTermsAndConditions(taxIdentifier: TaxIdentifier, termsAccepted: (TermsType, TermsAccepted), email: Option[String])(implicit hc: HeaderCarrier): Future[PreferencesStatus] =
    withCircuitBreaker(http.POST(url(s"/preferences/$taxIdentifier/terms-and-conditions"), EntityResolverConnector.TermsAndConditionsUpdate.from(termsAccepted, email)))
      .map(_.status).map {
      case OK => PreferencesExists
      case CREATED => PreferencesCreated
    }

  private[connectors] def responseToEmailVerificationLinkStatus(response: Future[HttpResponse])(implicit hc: HeaderCarrier) =
    response.map(_ => EmailVerificationLinkResponse.Ok)
      .recover {
        case Upstream4xxResponse(_, GONE, _, _) => EmailVerificationLinkResponse.Expired
        case Upstream4xxResponse(_, CONFLICT, _, _) => EmailVerificationLinkResponse.WrongToken
        case (_: Upstream4xxResponse | _: NotFoundException | _: BadRequestException) => EmailVerificationLinkResponse.Error
      }
}