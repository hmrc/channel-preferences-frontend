/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package controllers.internal

import connectors.{ EntityResolverConnector, _ }
import controllers.ExternalUrlPrefixes
import controllers.auth.{ AuthenticatedRequest, WithAuthRetrievals }
import javax.inject.Inject
import model.{ Encrypted, FormType, HostContext }
import org.joda.time.DateTime
import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc.{ Action, AnyContent, MessagesControllerComponents, Result }
import uk.gov.hmrc.auth.core.AuthConnector
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
    extends FrontendController(mcc) with WithAuthRetrievals {

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

  def preferencesStatus(hostContext: HostContext): Action[AnyContent] = Action.async { implicit request =>
    withAuthenticatedRequest { implicit authenticatedRequest: AuthenticatedRequest[_] => implicit hc: HeaderCarrier =>
      _preferencesStatus(hostContext)

    }
  }

  def preferencesStatusBySvc(svc: String, token: String, hostContext: HostContext): Action[AnyContent] = Action.async {
    implicit request =>
      withAuthenticatedRequest { implicit authenticatedRequest: AuthenticatedRequest[_] => implicit hc: HeaderCarrier =>
        _preferencesStatusMtd(svc, token, hostContext)
      }
  }

  def legacyPreferencesStatus(formType: FormType, taxIdentifier: String, hostContext: HostContext): Action[AnyContent] =
    Action.async { implicit request =>
      withAuthenticatedRequest { implicit authenticatedRequest: AuthenticatedRequest[_] => implicit hc: HeaderCarrier =>
        _preferencesStatus(hostContext)
      }
    }

  private def _preferencesStatusMtd(svc: String, token: String, hostContext: HostContext)(
    implicit hc: HeaderCarrier): Future[Result] =
    entityResolverConnector.getPreferencesStatusByToken(svc, token) map {
      case Right(PreferenceNotFound(email)) =>
        val encryptedEmail = email.map(e => Encrypted(EmailAddress(e.email)))
        val redirectUrl = hostUrl + routes.ChoosePaperlessController
          .redirectToDisplayFormWithCohortBySvc(svc, token, encryptedEmail, hostContext)
          .url
        PreconditionFailed(Json.obj("redirectUserTo" -> redirectUrl))
      case Right(PreferenceFound(true, emailPreference, _)) =>
        Ok(
          Json.obj(
            "optedIn"       -> true,
            "verifiedEmail" -> emailPreference.fold(false)(_.isVerified)
          ))
      case Right(PreferenceFound(false, email, _)) =>
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

  private def _preferencesStatus(hostContext: HostContext)(implicit hc: HeaderCarrier): Future[Result] = {

    val terms = hostContext.termsAndConditions.getOrElse("generic")
    entityResolverConnector.getPreferencesStatus(terms) map {
      case Right(PreferenceFound(true, emailPreference, updatedAt)) if hostContext.alreadyOptedInUrl.isDefined =>
        Redirect(hostContext.alreadyOptedInUrl.get)
      case Right(PreferenceFound(true, emailPreference, _)) =>
        Ok(
          Json.obj(
            "optedIn"       -> true,
            "verifiedEmail" -> emailPreference.fold(false)(_.isVerified)
          ))
      case Right(PreferenceFound(false, None, updatedAt)) =>
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

      case Right(PreferenceFound(false, email, updatedAt)) =>
        Ok(
          Json.obj( //
            "optedIn" -> false))
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
  }
}
