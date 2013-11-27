package controllers.bt

import uk.gov.hmrc.common.microservice.domain.User
import play.api.mvc.{Call, Request}
import controllers.common.actions.HeaderCarrier
import play.api.data._
import play.api.mvc.Results._
import play.api.data.Forms._
import uk.gov.hmrc.common.microservice.email.EmailConnector
import uk.gov.hmrc.common.microservice.preferences.PreferencesConnector
import play.api.http.Writeable

trait EmailControllerHelper {


  protected val emailForm: Form[EmailPreferenceData] = Form[EmailPreferenceData](mapping(
    "email" -> email,
    "emailVerified" -> optional(text)
  )(EmailPreferenceData.apply)(EmailPreferenceData.unapply))


  protected def submitPreferencesForm(errorsView: (Form[EmailPreferenceData]) => play.api.templates.HtmlFormat.Appendable,
                                      emailWarningView: (String) => play.api.templates.HtmlFormat.Appendable,
                                      successRedirect: () => Call,
                                      emailConnector: EmailConnector, preferencesConnector: PreferencesConnector)(implicit user: User, request: Request[AnyRef]) = {
    implicit def hc = HeaderCarrier(request)
    emailForm.bindFromRequest()(request).fold(
      errors => BadRequest(errorsView(errors)),
      emailForm => {
        if (emailForm.isEmailVerified || emailConnector.validateEmailAddress(emailForm.email)) {
          preferencesConnector.savePreferences(user.getSa.utr, true, Some(emailForm.email))
          Redirect(successRedirect())
        } else {
          Ok(emailWarningView(emailForm.email))
        }
      }
    )
  }

}

case class EmailPreferenceData(email: String, emailVerified: Option[String]) {
  lazy val isEmailVerified = emailVerified == Some("true")
}