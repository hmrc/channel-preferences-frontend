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
import ExecutionContext.Implicits.global

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
                                      preferencesConnector: PreferencesConnector)(implicit user: User, request: Request[AnyRef]): Future[SimpleResult] = {
    implicit def hc = HeaderCarrier(request)

    def savePreferencesAndRedirect(email: String): Future[SimpleResult] =
      preferencesConnector.savePreferences(user.getSa.utr, true, Some(email)).map { _ =>
        Redirect(successRedirect())
    }

    emailForm.bindFromRequest()(request).fold(
      errors => Future.successful(BadRequest(errorsView(errors))),
      emailForm => {
        if (emailForm.isEmailVerified) {
          savePreferencesAndRedirect(emailForm.mainEmail)
        } else  {
          emailConnector.validateEmailAddress(emailForm.mainEmail).flatMap { isValid =>
            if (isValid)
              savePreferencesAndRedirect(emailForm.mainEmail)
            else Future.successful(Ok(emailWarningView(emailForm.mainEmail)))
          }
        }
      }
    )
  }

}

case class EmailPreferenceData(email: (String, Option[String]), emailVerified: Option[String]) {
  lazy val isEmailVerified = emailVerified == Some("true")

  def mainEmail = email._1
}