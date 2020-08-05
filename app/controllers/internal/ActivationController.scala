/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package controllers.internal

import connectors.{ EntityResolverConnector, _ }
import controllers.ExternalUrlPrefixes
import controllers.auth.{ AuthenticatedRequest, WithAuthRetrievals }
import javax.inject.Inject
import model.{ Encrypted, FormType, HostContext, Language }
import org.joda.time.DateTime
import play.api.Configuration
import play.api.i18n.I18nSupport
import play.api.libs.json.Json
import play.api.mvc.{ Action, AnyContent, MessagesControllerComponents, Result }
import uk.gov.hmrc.auth.core.{ AffinityGroup, AuthConnector }
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.config.RunMode
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.{ ExecutionContext, Future }

class ActivationController @Inject()(
  entityResolverConnector: EntityResolverConnector,
  val authConnector: AuthConnector,
  externalUrlPrefixes: ExternalUrlPrefixes,
  mcc: MessagesControllerComponents,
  runMode: RunMode,
  config: Configuration
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with WithAuthRetrievals with I18nSupport with LanguageHelper {

  val hostUrl = externalUrlPrefixes.pfUrlPrefix

  private lazy val gracePeriod =
    config
      .getOptional[Int](s"${runMode.env}.activation.gracePeriodInMin")
      .getOrElse(throw new RuntimeException(s"missing ${runMode.env}.activation.gracePeriodInMin"))

  def preferences(): Action[AnyContent] = Action.async { implicit request =>
    withAuthenticatedRequest { implicit authenticatedRequest: AuthenticatedRequest[_] => implicit hc: HeaderCarrier =>
      entityResolverConnector.getPreferences().map {
        case Some(preference) => Ok(Json.toJson(preference))
        case _                => NotFound
      }
    }
  }

  def activate(hostContext: HostContext): Action[AnyContent] = Action.async { implicit request =>
    withAuthenticatedRequest { implicit authenticatedRequest: AuthenticatedRequest[_] => implicit hc: HeaderCarrier =>
      {
        val terms = hostContext.termsAndConditions.getOrElse("generic")
        for {
          preferenceStatus <- entityResolverConnector.getPreferencesStatus(terms)
          lang = languageType(request.lang.code)
          _ <- _storeLanguagePreference(hostContext, lang, preferenceStatus)
        } yield _preferencesStatusResult(hostContext, preferenceStatus)
      }
    }
  }

  def activateFromToken(svc: String, token: String, hostContext: HostContext): Action[AnyContent] = Action.async {
    implicit request =>
      withAuthenticatedRequest { implicit authenticatedRequest: AuthenticatedRequest[_] => implicit hc: HeaderCarrier =>
        {
          for {
            preferenceStatus <- entityResolverConnector.getPreferencesStatusByToken(svc, token)
            lang = languageType(request.lang.code)
            _ <- _storeLanguagePreference(hostContext, lang, preferenceStatus)
          } yield _preferencesStatusResultMtd(svc, token, hostContext, preferenceStatus)
        }
      }
  }

  def activateLegacyFromTaxIdentifier(
    formType: FormType,
    taxIdentifier: String,
    hostContext: HostContext): Action[AnyContent] = activate(hostContext)

  private def _storeLanguagePreference(
    hostContext: HostContext,
    lang: Language,
    preferenceStatus: Either[Int, PreferenceStatus])(implicit hc: HeaderCarrier): Future[Unit] =
    preferenceStatus match {
      case Right(PreferenceFound(true, emailPreference, _, _, _)) if emailPreference.exists(_.language.isEmpty) => {
        entityResolverConnector
          .updateTermsAndConditions(TermsAndConditionsUpdate.fromLanguage(Some(lang)))(hc, hostContext)
          .map(_ => ())
      }
      case _ => Future.successful(())
    }

  private def _preferencesStatusResultMtd(
    svc: String,
    token: String,
    hostContext: HostContext,
    preferenceStatus: Either[Int, PreferenceStatus])(implicit hc: HeaderCarrier): Result =
    preferenceStatus match {
      case Right(PreferenceNotFound(email)) =>
        val encryptedEmail = email.map(e => Encrypted(EmailAddress(e.email)))
        val redirectUrl = hostUrl + routes.ChoosePaperlessController
          .redirectToDisplayFormWithCohortBySvc(svc, token, encryptedEmail, hostContext)
          .url
        PreconditionFailed(Json.obj("redirectUserTo" -> redirectUrl))
      case Right(PreferenceFound(true, emailPreference, _, None, _)) =>
        Ok(
          Json.obj(
            "optedIn"       -> true,
            "verifiedEmail" -> emailPreference.fold(false)(_.isVerified)
          ))

      case Right(PreferenceFound(false, email, _, _, _)) =>
        val encryptedEmail = email.map(e => Encrypted(EmailAddress(e.email)))
        val redirectUrl = hostUrl + routes.ChoosePaperlessController
          .redirectToDisplayFormWithCohortBySvc(svc, token, encryptedEmail, hostContext)
          .url
        Ok(
          Json.obj(
            "optedIn"        -> false,
            "redirectUserTo" -> redirectUrl
          ))
      case _ => NotFound
    }

  private def _preferencesStatusResult(hostContext: HostContext, preferenceStatus: Either[Int, PreferenceStatus])(
    implicit hc: HeaderCarrier,
    authenticatedRequest: AuthenticatedRequest[_]): Result =
    preferenceStatus match {
      case Right(PreferenceFound(true, emailPreference, updatedAt, _, _))
          if hostContext.alreadyOptedInUrl.isDefined => {
        Redirect(hostContext.alreadyOptedInUrl.get)
      }
      case Right(PreferenceFound(true, emailPreference, _, None, _)) =>
        Ok(
          Json.obj(
            "optedIn"       -> true,
            "verifiedEmail" -> emailPreference.fold(false)(_.isVerified)
          ))

      case Right(PreferenceFound(true, emailPreference, _, Some(majorVersion), paperless)) =>
        if (triggerReOptIn(authenticatedRequest, emailPreference, majorVersion, paperless)) {
          val encryptedEmail = None
          val redirectUrl = hostUrl + routes.ChoosePaperlessController
            .displayForm(
              Some(CohortCurrent.reoptinpage),
              encryptedEmail,
              hostContext.copy(cohort = Some(CohortCurrent.reoptinpage), email = emailPreference.map(_.email)))
            .url
          PreconditionFailed(Json.obj("redirectUserTo" -> redirectUrl))
        } else {
          Ok(
            Json.obj(
              "optedIn"       -> true,
              "verifiedEmail" -> emailPreference.fold(false)(_.isVerified)
            ))

        }
      case Right(PreferenceFound(false, None, updatedAt, _, _)) =>
        updatedAt
          .flatMap(u =>
            if (u.plusMinutes(gracePeriod).isAfter(DateTime.now)) {
              Some(
                Ok(
                  Json.obj(
                    "optedIn" -> false
                  )))
            } else None)
          .getOrElse {

            val encryptedEmail = None
            val redirectUrl = hostUrl + routes.ChoosePaperlessController
              .redirectToDisplayFormWithCohort(encryptedEmail, hostContext)
              .url
            PreconditionFailed(Json.obj("redirectUserTo" -> redirectUrl))
          }
      case Right(PreferenceFound(false, email, updatedAt, _, _)) =>
        Ok(Json.obj("optedIn" -> false))
      case Right(PreferenceNotFound(Some(email))) if (hostContext.email.exists(_ != email.email)) =>
        Conflict
      case Right(PreferenceNotFound(email)) =>
        val encryptedEmail = email.map(e => Encrypted(EmailAddress(e.email)))
        val redirectUrl = hostUrl + routes.ChoosePaperlessController
          .redirectToDisplayFormWithCohort(encryptedEmail, hostContext)
          .url
        PreconditionFailed(Json.obj("redirectUserTo" -> redirectUrl))
      case Left(status) => Status(status)
    }

  private def triggerReOptIn(
    authenticatedRequest: AuthenticatedRequest[_],
    emailPreference: Option[EmailPreference],
    majorVersion: Int,
    paperless: Option[Boolean]) = {
    val versionBehind = majorVersion < CohortCurrent.ipage.majorVersion
    val isIndividual = authenticatedRequest.affinityGroup.exists(_ == AffinityGroup.Individual)
    val individualConfidenceLevel = authenticatedRequest.confidenceLevel.exists(_.level == 200)
    val noPendingEmail = emailPreference.exists(_.pendingEmail.isEmpty)
    val isPaperless = paperless.exists(identity)
    versionBehind && isIndividual && individualConfidenceLevel && noPendingEmail && isPaperless
  }
}
