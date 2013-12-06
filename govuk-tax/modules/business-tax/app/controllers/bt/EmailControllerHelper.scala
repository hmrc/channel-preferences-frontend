package controllers.bt

import uk.gov.hmrc.common.microservice.domain.User
import play.api.mvc.{Call, Request}
import controllers.common.actions.HeaderCarrier
import play.api.data._
import play.api.mvc.Results._
import play.api.data.Forms._
import uk.gov.hmrc.common.microservice.email.EmailConnector
import uk.gov.hmrc.common.microservice.preferences.PreferencesConnector

trait EmailControllerHelper {


  protected val emailForm: Form[EmailPreferenceData] = Form[EmailPreferenceData](mapping(
    "email" -> tuple(
      "main" -> email,
      "confirm" -> optional(text)
    ).verifying(
      "email.confirmation.emails.unequal", email => email._1 == email._2.getOrElse("")
    ),
    "emailVerified" -> optional(text)
  )(EmailPreferenceData.apply)(EmailPreferenceData.unapply))


  protected def submitPreferencesForm(errorsView: (Form[EmailPreferenceData]) => play.api.templates.HtmlFormat.Appendable,
                                      emailWarningView: (String) => play.api.templates.HtmlFormat.Appendable,
                                      successRedirect: () => Call,
                                      emailConnector: EmailConnector,
                                      preferencesConnector: PreferencesConnector)(implicit user: User, request: Request[AnyRef]) = {
    implicit def hc = HeaderCarrier(request)
    emailForm.bindFromRequest()(request).fold(
      errors => BadRequest(errorsView(errors)),
      emailForm => {
        if (emailForm.isEmailVerified || emailConnector.validateEmailAddress(emailForm.mainEmail)) {
          preferencesConnector.savePreferences(user.getSa.utr, true, Some(emailForm.mainEmail))
          Redirect(successRedirect())
        } else {
          Ok(emailWarningView(emailForm.mainEmail))
        }
      }
    )
  }

}

case class EmailPreferenceData(email: (String, Option[String]), emailVerified: Option[String]) {
  lazy val isEmailVerified = emailVerified == Some("true")

  def mainEmail = email._1
}