package connectors

import config.ServicesCircuitBreaker
import model.{HostContext, ReturnLink}
import play.api.http.Status
import play.api.libs.json._
import uk.gov.hmrc.domain.{Nino, SaUtr, TaxIdentifier}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import scala.concurrent.Future
import uk.gov.hmrc.http._

case class Email(email: String)

object Email {
  implicit val readsEmail = Json.reads[Email]
}

sealed trait TermsType

case object GenericTerms extends TermsType

case object TaxCreditsTerms extends TermsType

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

  protected[connectors] case class TermsAndConditionsUpdate(generic: Option[TermsAccepted], taxCredits: Option[TermsAccepted], email: Option[String], returnUrl: Option[String] = None, returnText: Option[String] = None)

  protected[connectors] object TermsAndConditionsUpdate {
    implicit val format = Json.format[TermsAndConditionsUpdate]

    def from(terms: (TermsType, TermsAccepted), email: Option[String], includeLinkDetails: Boolean)(implicit hostContext: HostContext): TermsAndConditionsUpdate = {
      val standardConditions = terms match {
        case (GenericTerms, accepted: TermsAccepted) => TermsAndConditionsUpdate(generic = Some(accepted), None, email = email)
        case (TaxCreditsTerms, accepted: TermsAccepted) => TermsAndConditionsUpdate(generic = None, taxCredits = Some(accepted), email = email)
        case (termsType, _) => throw new IllegalArgumentException(s"Could not work with termsType=$termsType")
      }
      if (includeLinkDetails)
        standardConditions.copy(returnText = Some(hostContext.returnLinkText), returnUrl = Some(hostContext.returnUrl))
      else
        standardConditions
    }
  }
}

trait PreferenceStatus
case class PreferenceFound(accepted: Boolean, email: Option[EmailPreference]) extends PreferenceStatus
case class PreferenceNotFound(email: Option[EmailPreference]) extends PreferenceStatus


trait EntityResolverConnector extends Status with ServicesCircuitBreaker {
  this: ServicesConfig =>

  val externalServiceName = "entity-resolver"

  def http: HttpGet with HttpPost with HttpPut

  def serviceUrl: String

  def url(path: String) = s"$serviceUrl$path"

  def getPreferencesStatus(termsAndCond: String = "generic")(implicit headerCarrier: HeaderCarrier): Future[Either[Int, PreferenceStatus]] = {
    getPreferencesStatus_Final(termsAndCond, url(s"/preferences"))
  }

  def getPreferencesStatusByToken(svc: String, token: String, termsAndCond: String = "generic")(implicit headerCarrier: HeaderCarrier): Future[Either[Int, PreferenceStatus]] = {
    getPreferencesStatus_Final(termsAndCond, url(s"/preferences/$svc/$token"))
  }

  def getPreferencesStatus_Final(termsAndCond: String, request_url : String)(implicit headerCarrier: HeaderCarrier): Future[Either[Int, PreferenceStatus]] = {
    withCircuitBreaker({
      http.GET[Option[PreferenceResponse]](request_url).map {
        case Some(preference) =>
          preference.termsAndConditions.get(termsAndCond).fold[Either[Int, PreferenceStatus]](
            Right(PreferenceNotFound(preference.email))) {
            acceptance => Right(PreferenceFound(acceptance.accepted, preference.email))
          }
        case None => Right(PreferenceNotFound(None))
      }
    }).recover {
      case response: Upstream4xxResponse => response.upstreamResponseCode match {
        case NOT_FOUND => Left(NOT_FOUND)
        case UNAUTHORIZED => Left(UNAUTHORIZED)
      }
      case response: BadRequestException => Left(BAD_REQUEST)
    }
  }

  def changeEmailAddress(newEmail: String)(implicit hc: HeaderCarrier): Future[HttpResponse] =
    withCircuitBreaker(http.PUT(url(s"/preferences/pending-email"), UpdateEmail(newEmail)))

  def getPreferences()(implicit headerCarrier: HeaderCarrier): Future[Option[PreferenceResponse]] =
    withCircuitBreaker {
      http.GET[Option[PreferenceResponse]](url(s"/preferences"))
    }.recover {
      case response: Upstream4xxResponse if response.upstreamResponseCode == GONE => None
      case e: NotFoundException => None
    }

  def getEmailAddress(taxId: TaxIdentifier)(implicit hc: HeaderCarrier) = {
    def basedOnTaxIdType = taxId match {
      case SaUtr(utr) => s"/portal/preferences/sa/$utr/verified-email-address"
      case Nino(nino) => s"/portal/preferences/paye/$nino/verified-email-address"
    }

    withCircuitBreaker(http.GET[Option[Email]](url(basedOnTaxIdType))).map(_.map(_.email))
  }

  def updateEmailValidationStatusUnsecured(token: String)(implicit hc: HeaderCarrier): Future[EmailVerificationLinkResponse] = {
    responseToEmailVerificationLinkStatus(withCircuitBreaker(http.PUT(url("/portal/preferences/email"), ValidateEmail(token))))
  }

  def updateTermsAndConditions(termsAccepted: (TermsType, TermsAccepted), email: Option[String])(implicit hc: HeaderCarrier, hostContext: HostContext): Future[PreferencesStatus] =
    updateTermsAndConditionsForSvc(termsAccepted, email, None, None, false)

  def updateTermsAndConditionsForSvc(termsAccepted: (TermsType, TermsAccepted), email: Option[String], svc: Option[String], token: Option[String], includeLinkDetails: Boolean = true)(implicit hc: HeaderCarrier, hostContext: HostContext): Future[PreferencesStatus] = {
    val endPoint = "/preferences/terms-and-conditions" + (for {
      s <- svc
      t <- token
    } yield "/" + s + "/" + t).getOrElse("")
    withCircuitBreaker(http.POST(url(endPoint), Json.toJson(EntityResolverConnector.TermsAndConditionsUpdate.from(termsAccepted, email, includeLinkDetails))))
      .map(_.status).map {
      case OK => PreferencesExists
      case CREATED => PreferencesCreated
    }
  }

  private[connectors] def responseToEmailVerificationLinkStatus(response: Future[HttpResponse])(implicit hc: HeaderCarrier) = {
    response.map(response => {
      response.status match {
        case CREATED =>
          val link = ReturnLink.fromString(response.body)
          ValidatedWithReturn(link.linkText, link.linkUrl)
        case _ => Validated
      }
    }).recover {
      case Upstream4xxResponse(_, GONE, _, _) => ValidationExpired
      case Upstream4xxResponse(_, CONFLICT, _, _) => WrongToken
      case Upstream4xxResponse(messageBody, PRECONDITION_FAILED, _, _) =>
        val body = messageBody.substring(messageBody.indexOf("Response body: '") + 16).stripSuffix("'")
        val link = ReturnLink.fromString(body)
        ValidationErrorWithReturn(link.linkText, link.linkUrl)
      case (_: Upstream4xxResponse | _: NotFoundException | _: BadRequestException) => ValidationError
    }
  }
}