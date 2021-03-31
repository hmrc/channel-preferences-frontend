/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package connectors

import _root_.controllers.internal.OptInCohort
import config.ServicesCircuitBreaker
import model.{ HostContext, Language, PageType, ReturnLink, Survey }
import org.joda.time.DateTime
import play.api.http.Status._
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.{ Configuration, Environment }
import uk.gov.hmrc.domain.{ Nino, SaUtr, TaxIdentifier }
import uk.gov.hmrc.http.{ HttpClient, _ }
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import javax.inject.{ Inject, Singleton }
import scala.concurrent.Future
import _root_.model.SurveyType

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

import play.api.libs.json.JodaWrites.{ JodaDateTimeWrites => _ }

case class Version(major: Int, minor: Int)

object Version {
  implicit val versionFormat: OFormat[Version] = Json.format[Version]
}

case class OptInPage private (version: Version, cohort: OptInCohort, pageType: PageType)

object OptInPage {
  implicit val optInPageWrites: Writes[OptInPage] = Json.writes[OptInPage]
  def from(cohort: OptInCohort): OptInPage =
    OptInPage(Version(cohort.majorVersion, cohort.minorVersion), cohort, cohort.pageType)

  implicit val optInPageReads: Reads[OptInPage] = ((__ \ "version").read[Version] and
    Reads[OptInCohort](j =>
      (__ \ "cohort").asSingleJson(j) match {
        case JsDefined(value) =>
          value
            .validate[Int]
            .flatMap(v =>
              OptInCohort
                .fromId(v)
                .fold[JsResult[OptInCohort]](
                  JsError(Seq(JsPath -> Seq(JsonValidationError(s"invalid cohort number $value"))))
                )(c => JsSuccess(c))
            )
        case JsUndefined() => JsError(Seq(JsPath -> Seq(JsonValidationError("missing cohort"))))
      }
    )
    and
      (__ \ "pageType").read[PageType])((ver, cohort, pageType) =>
    if (ver.major == cohort.majorVersion && ver.minor == cohort.minorVersion && cohort.pageType == pageType)
      OptInPage(ver, cohort, pageType)
    else
      throw new IllegalArgumentException("Invalid constructor arguments")
  )
  implicit val optInPageFormat: Format[OptInPage] = Format(optInPageReads, optInPageWrites)
}

case class TermsAccepted(accepted: Boolean, optInPage: Option[OptInPage] = None, surveyType: Option[SurveyType] = None)

object TermsAccepted {
  implicit val format = Json.format[TermsAccepted]
}

trait PreferenceStatus
case class PreferenceFound(
  accepted: Boolean,
  email: Option[EmailPreference],
  updatedAt: Option[DateTime] = None,
  majorVersion: Option[Int] = None,
  paperless: Option[Boolean],
  surveys: Option[List[Survey]] = None
) extends PreferenceStatus
case class PreferenceNotFound(email: Option[EmailPreference]) extends PreferenceStatus

sealed abstract case class TermsAndConditionsUpdate(
  generic: Option[TermsAccepted],
  taxCredits: Option[TermsAccepted],
  email: Option[String],
  returnUrl: Option[String] = None,
  returnText: Option[String] = None,
  language: Option[Language]
)

object TermsAndConditionsUpdate {
  def from(
    terms: (TermsType, TermsAccepted),
    email: Option[String],
    includeLinkDetails: Boolean,
    language: Some[Language]
  )(implicit hostContext: HostContext): TermsAndConditionsUpdate =
    terms match {
      case (GenericTerms, accepted) if includeLinkDetails =>
        new TermsAndConditionsUpdate(
          Some(accepted),
          None,
          email,
          Some(hostContext.returnLinkText),
          Some(hostContext.returnUrl),
          language
        ) {}
      case (TaxCreditsTerms, accepted) if includeLinkDetails =>
        new TermsAndConditionsUpdate(
          None,
          Some(accepted),
          email,
          Some(hostContext.returnLinkText),
          Some(hostContext.returnUrl),
          language
        ) {}
      case (GenericTerms, accepted) =>
        new TermsAndConditionsUpdate(Some(accepted), None, email, None, None, language) {}
      case (TaxCreditsTerms, accepted) =>
        new TermsAndConditionsUpdate(None, Some(accepted), email, None, None, language) {}
      case (termsType, _) =>
        throw new IllegalArgumentException(s"Could not work with termsType=$termsType")
    }
  def fromLanguage(language: Some[Language]): TermsAndConditionsUpdate =
    new TermsAndConditionsUpdate(None, None, None, None, None, language) {}

  implicit val writes: Writes[TermsAndConditionsUpdate] = (
    (JsPath \ "generic").write[Option[TermsAccepted]] and
      (JsPath \ "taxCredits").write[Option[TermsAccepted]] and
      (JsPath \ "email").write[Option[String]] and
      (JsPath \ "returnUrl").write[Option[String]] and
      (JsPath \ "returnText").write[Option[String]] and
      (JsPath \ "language").write[Option[Language]]
  )(unlift(TermsAndConditionsUpdate.unapply))
}
@Singleton
class EntityResolverConnector @Inject() (config: Configuration, env: Environment, http: HttpClient)
    extends ServicesConfig(config) with ServicesCircuitBreaker {

  override val externalServiceName = "entity-resolver"
  val serviceUrl = baseUrl("entity-resolver")

  protected[connectors] case class ActivationStatus(active: Boolean)

  protected[connectors] object ActivationStatus {
    implicit val format = Json.format[ActivationStatus]
  }

  def url(path: String) = s"$serviceUrl$path"

  def getPreferencesStatus(
    termsAndCond: String = "generic"
  )(implicit headerCarrier: HeaderCarrier): Future[Either[Int, PreferenceStatus]] =
    getPreferencesStatus_Final(termsAndCond, url(s"/preferences"))

  def getPreferencesStatusByToken(svc: String, token: String, termsAndCond: String = "generic")(implicit
    headerCarrier: HeaderCarrier
  ): Future[Either[Int, PreferenceStatus]] =
    getPreferencesStatus_Final(termsAndCond, url(s"/preferences/$svc/$token"))

  def getPreferencesStatus_Final(termsAndCond: String, request_url: String)(implicit
    headerCarrier: HeaderCarrier
  ): Future[Either[Int, PreferenceStatus]] =
    withCircuitBreaker {
      http.GET[Option[PreferenceResponse]](request_url).map {
        case Some(preference) =>
          preference.termsAndConditions
            .get(termsAndCond)
            .fold[Either[Int, PreferenceStatus]](Right(PreferenceNotFound(preference.email))) { acceptance =>
              Right(
                PreferenceFound(
                  acceptance.accepted,
                  preference.email,
                  acceptance.updatedAt,
                  acceptance.majorVersion,
                  paperless = acceptance.paperless,
                  surveys = preference.surveys
                )
              )
            }
        case None => Right(PreferenceNotFound(None))
      }
    }.recover {
      case response: Upstream4xxResponse =>
        response.upstreamResponseCode match {
          case NOT_FOUND    => Left(NOT_FOUND)
          case UNAUTHORIZED => Left(UNAUTHORIZED)
        }
      case response: BadRequestException => Left(BAD_REQUEST)
    }

  def changeEmailAddress(newEmail: String)(implicit hc: HeaderCarrier): Future[HttpResponse] =
    withCircuitBreaker(http.PUT(url(s"/preferences/pending-email"), UpdateEmail(newEmail)))

  def getPreferences()(implicit headerCarrier: HeaderCarrier): Future[Option[PreferenceResponse]] =
    withCircuitBreaker {
      http.GET[Option[PreferenceResponse]](url(s"/preferences"))
    }.recover {
      case response: Upstream4xxResponse if response.upstreamResponseCode == GONE => None
      case e: NotFoundException                                                   => None
    }

  def getEmailAddress(taxId: TaxIdentifier)(implicit hc: HeaderCarrier): Future[Option[String]] = {
    def basedOnTaxIdType =
      taxId match {
        case SaUtr(utr) => s"/portal/preferences/sa/$utr/verified-email-address"
        case Nino(nino) => s"/portal/preferences/paye/$nino/verified-email-address"
      }

    withCircuitBreaker(http.GET[Option[Email]](url(basedOnTaxIdType))).map(_.map(_.email))
  }

  def updateEmailValidationStatusUnsecured(
    token: String
  )(implicit hc: HeaderCarrier): Future[EmailVerificationLinkResponse] =
    responseToEmailVerificationLinkStatus(
      withCircuitBreaker(http.PUT(url("/portal/preferences/email"), ValidateEmail(token)))
    )

  def updateTermsAndConditions(
    termsAndConditionsUpdate: TermsAndConditionsUpdate
  )(implicit hc: HeaderCarrier, hostContext: HostContext): Future[PreferencesStatus] =
    updateTermsAndConditionsForSvc(termsAndConditionsUpdate, None, None)

  def updateTermsAndConditionsForSvc(
    termsAndConditionsUpdate: TermsAndConditionsUpdate,
    svc: Option[String],
    token: Option[String]
  )(implicit hc: HeaderCarrier, hostContext: HostContext): Future[PreferencesStatus] = {
    val endPoint = "/preferences/terms-and-conditions" + (for {
      s <- svc
      t <- token
    } yield "/" + s + "/" + t).getOrElse("")
    withCircuitBreaker(http.POST[TermsAndConditionsUpdate, HttpResponse](url(endPoint), termsAndConditionsUpdate))
      .map(_.status)
      .map {
        case OK      => PreferencesExists
        case CREATED => PreferencesCreated
      }
  }

  private[connectors] def responseToEmailVerificationLinkStatus(
    response: Future[HttpResponse]
  )(implicit hc: HeaderCarrier): Future[EmailVerificationLinkResponse] =
    response
      .map { response =>
        response.status match {
          case CREATED =>
            val link = ReturnLink.fromString(response.body)
            ValidatedWithReturn(link.linkText, link.linkUrl)
          case _ => Validated
        }
      }
      .recover {
        case Upstream4xxResponse(_, GONE, _, _)     => ValidationExpired
        case Upstream4xxResponse(_, CONFLICT, _, _) => WrongToken
        case Upstream4xxResponse(messageBody, PRECONDITION_FAILED, _, _) =>
          val body = messageBody.substring(messageBody.indexOf("Response body: '") + 16).stripSuffix("'")
          val link = ReturnLink.fromString(body)
          ValidationErrorWithReturn(link.linkText, link.linkUrl)
        case (_: Upstream4xxResponse | _: NotFoundException | _: BadRequestException) => ValidationError
      }
}
