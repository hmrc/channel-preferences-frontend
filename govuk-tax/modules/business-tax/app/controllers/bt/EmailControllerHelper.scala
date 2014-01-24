package controllers.bt

import uk.gov.hmrc.common.microservice.domain.User
import play.api.mvc.{SimpleResult, Call, Request}
import controllers.common.actions.HeaderCarrier
import play.api.data._
import play.api.mvc.Results._
import play.api.data.Forms._
import uk.gov.hmrc.common.microservice.email.EmailConnector
import uk.gov.hmrc.common.microservice.preferences.PreferencesConnector
import scala.concurrent._
import uk.gov.hmrc.common.MdcLoggingExecutionContext.fromLoggingDetails
import Function.const
import uk.gov.hmrc.domain.Email

trait EmailControllerHelper {


  protected val emailForm: Form[EmailPreferenceData] = Form[EmailPreferenceData](mapping(
    "email" ->
      tuple(
        "main" -> email.verifying("error.email_too_long", email => email.size < 320),
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
                                      preferencesConnector: PreferencesConnector)(implicit user: User, request: Request[AnyRef]): Future[SimpleResult] = {
    implicit def hc = HeaderCarrier(request)

    emailForm.bindFromRequest()(request).fold(
      errors => Future.successful(BadRequest(errorsView(errors))),
      emailForm => {
        val isEmailValid = if (emailForm.isEmailVerified)
          Future.successful(true)
        else
          emailConnector.validateEmailAddress(emailForm.mainEmail)

        isEmailValid.flatMap {
           case true => preferencesConnector.savePreferences(user.getSa.utr, true, Some(emailForm.mainEmail)).map(const(Redirect(successRedirect())))
           case false => Future.successful(Ok(emailWarningView(emailForm.mainEmail)))
        }
      }
    )
  }

}

case class EmailPreferenceData(email: (String, Option[String]), emailVerified: Option[String]) {
  lazy val isEmailVerified = emailVerified == Some("true")

  def mainEmail = email._1
}
object EmailPreferenceData {
  def apply(emailAddress: Option[Email]): EmailPreferenceData = {
    EmailPreferenceData(emailAddress.map(e => (e.value, Some(e.value))).getOrElse(("", None)), None)
  }
}