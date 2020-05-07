/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package controllers

import connectors.{ Email, EntityResolverConnector, PreferenceResponse }
import controllers.auth.{ AuthenticatedRequest, WithAuthRetrievals }
import javax.inject.Inject
import model.StatusName.{ Alright, BouncedEmail, EmailNotVerified, NewCustomer, NoEmail, Paper }
import model._
import play.api.i18n.{ I18nSupport, Lang }
import play.api.libs.json.Json
import play.api.mvc.{ Action, AnyContent, MessagesControllerComponents }
import service.PaperlessStatusService
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.ExecutionContext

class PaperlessStatusController @Inject()(
  entityResolverConnector: EntityResolverConnector,
  val authConnector: AuthConnector,
  mcc: MessagesControllerComponents,
  statusService: PaperlessStatusService,
  externalUrlPrefixes: ExternalUrlPrefixes)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with I18nSupport with WithAuthRetrievals {

  def getPaperlessStatus(implicit hostContext: HostContext): Action[AnyContent] = Action.async { implicit request =>
    withAuthenticatedRequest { implicit authenticatedRequest: AuthenticatedRequest[_] => implicit hc: HeaderCarrier =>
      entityResolverConnector
        .getPreferences()
        .map(pref => Ok(Json.toJson(determinePaperlessStatus(pref)(request.lang, hostContext))))
    }(request, ec)
  }

  private def determinePaperlessStatus(preferenceResponse: Option[PreferenceResponse])(
    implicit language: Lang,
    hostContext: HostContext): StatusWithUrl = {

    val checkSettingsUrl = externalUrlPrefixes.pfUrlPrefix + controllers.internal.routes.ManagePaperlessController
      .checkSettings(hostContext)

    val getEmail = hostContext.email match {
      case Some(email) => Some(Encrypted(EmailAddress(email)))
      case None        => None
    }

    val optInRedirect = externalUrlPrefixes.pfUrlPrefix + controllers.internal.routes.ChoosePaperlessController
      .redirectToDisplayFormWithCohort(getEmail, hostContext)

    statusService.determineStatus(preferenceResponse) match {
      case Paper =>
        StatusWithUrl(
          PaperlessStatus(Paper, Category(Paper), messagesApi("paperless.status.text.paper")),
          Url(checkSettingsUrl, messagesApi("paperless.status.url.text.paper"))
        )
      case EmailNotVerified =>
        StatusWithUrl(
          PaperlessStatus(
            EmailNotVerified,
            Category(EmailNotVerified),
            messagesApi("paperless.status.text.email_not_verified")),
          Url(checkSettingsUrl, messagesApi("paperless.status.url.text.email_not_verified"))
        )
      case BouncedEmail =>
        StatusWithUrl(
          PaperlessStatus(BouncedEmail, Category(BouncedEmail), messagesApi("paperless.status.text.bounced")),
          Url(checkSettingsUrl, messagesApi("paperless.status.url.text.bounced"))
        )
      case NewCustomer =>
        StatusWithUrl(
          PaperlessStatus(NewCustomer, Category(NewCustomer), messagesApi("paperless.status.text.new_customer")),
          Url(optInRedirect, messagesApi("paperless.status.url.text.new_customer"))
        )
      case Alright =>
        StatusWithUrl(
          PaperlessStatus(Alright, Category(Alright), messagesApi("paperless.status.text.alright")),
          Url(checkSettingsUrl, messagesApi("paperless.status.url.text.alright"))
        )
      case NoEmail =>
        StatusWithUrl(
          PaperlessStatus(NoEmail, Category(NoEmail), messagesApi("paperless.status.text.no_email")),
          Url(checkSettingsUrl, messagesApi("paperless.status.url.text.no_email"))
        )
    }
  }
}
