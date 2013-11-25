package controllers.bt.prefs

import play.api.data._
import play.api.data.Forms._
import play.api.mvc.Request
import uk.gov.hmrc.common.microservice.preferences.PreferencesConnector
import controllers.common.{FrontEndRedirect, BaseController}
import controllers.common.actions.{HeaderCarrier, Actions}
import controllers.common.service.Connectors
import uk.gov.hmrc.common.microservice.audit.AuditConnector
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import uk.gov.hmrc.common.microservice.sa.domain.SaRegime
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.common.microservice.email.EmailConnector
import controllers.bt.BusinessTaxRegimeRoots

class SaPrefsController(override val auditConnector: AuditConnector, preferencesConnector: PreferencesConnector, emailConnector: EmailConnector)
                       (implicit override val authConnector: AuthConnector)
      extends BaseController
      with Actions
      with BusinessTaxRegimeRoots {


  def this() = this(Connectors.auditConnector, Connectors.preferencesConnector, Connectors.emailConnector)(Connectors.authConnector)

  private val emailForm: Form[EmailPreferenceData] = Form[EmailPreferenceData](mapping(
    "email" -> email,
    "emailVerified" -> optional(text)
  )(EmailPreferenceData.apply)(EmailPreferenceData.unapply))

  def displayPrefsForm(emailAddress: Option[String]) = AuthorisedFor(account = SaRegime) {
    user => request =>
      displayPrefsFormAction(emailAddress)(user, request)
  }

  def submitPrefsForm() = AuthorisedFor(account = SaRegime) {
    user => request =>
      submitPrefsFormAction(user, request)
  }

  def submitKeepPaperForm() = AuthorisedFor(account = SaRegime) {
    user => request =>
      submitKeepPaperFormAction(user, request)
  }

  def thankYou() = AuthorisedFor(account = SaRegime) {
    user => request =>
      Ok(views.html.sa_printing_preference_thank_you(user))
  }

  private[prefs] def displayPrefsFormAction(emailAddress: Option[String])(implicit user: User, request: Request[AnyRef]) = {
    preferencesConnector.getPreferences(user.getSa.utr)(HeaderCarrier(request)) match {
      case Some(saPreference) => FrontEndRedirect.toBusinessTax
      case _ => Ok(views.html.sa_printing_preference(emailForm.fill(EmailPreferenceData(emailAddress.getOrElse(""), None))))
    }
  }

  private[prefs] def submitPrefsFormAction(implicit user: User, request: Request[AnyRef]) = {
    implicit def hc = HeaderCarrier(request)
    emailForm.bindFromRequest()(request).fold(
      errors => BadRequest(views.html.sa_printing_preference(errors)),
      emailForm => {
        if (emailForm.isEmailVerified || emailConnector.validateEmailAddress(emailForm.email)) {
          preferencesConnector.savePreferences(user.getSa.utr, true, Some(emailForm.email))
          Redirect(routes.SaPrefsController.thankYou())
        } else {
          Ok(views.html.sa_printing_preference_verify_email(emailForm.email))
        }
      }
    )
  }

  private[prefs] def submitKeepPaperFormAction(implicit user: User, request: Request[AnyRef]) = {
    preferencesConnector.savePreferences(user.getSa.utr, false, None)(HeaderCarrier(request))
    Redirect(FrontEndRedirect.businessTaxHome)
  }
}

case class EmailPreferenceData(email: String, emailVerified: Option[String]) {
  lazy val isEmailVerified = emailVerified == Some("true")
}