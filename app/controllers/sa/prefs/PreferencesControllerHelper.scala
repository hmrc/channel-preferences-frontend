package controllers.sa.prefs

import play.api.mvc.{SimpleResult, Call, Request}
import controllers.common.actions.HeaderCarrier
import play.api.data._
import play.api.mvc.Results._
import play.api.data.Forms._
import uk.gov.hmrc.common.microservice.email.EmailConnector
import uk.gov.hmrc.common.microservice.preferences.FormattedUri
import scala.concurrent._
import uk.gov.hmrc.common.MdcLoggingExecutionContext.fromLoggingDetails
import Function.const
import uk.gov.hmrc.domain.{Email, SaUtr}
import play.api.templates.HtmlFormat

trait PreferencesControllerHelper {

  val emailWithLimitedLength: Mapping[String] = email.verifying("error.email_too_long", email => email.size < 320)


  // TODO the duplication of these forms is all wrong - need to alter the field names in the HTML to
  // be able to restructure and sort this out.
  protected val emailForm = Form[EmailFormData](mapping(
    "email" -> tuple(
      "main" -> emailWithLimitedLength,
      "confirm" -> optional(text)
    ).verifying("email.confirmation.emails.unequal", email => email._1 == email._2.getOrElse("")),
    "emailVerified" -> optional(text)
  )(EmailFormData.apply)(EmailFormData.unapply))

  protected val emailFormWithPreference =
    Form[EmailFormDataWithPreference](mapping(
      "email" -> tuple(
        "main" -> optional(emailWithLimitedLength),
        "confirm" -> optional(text)
      ).verifying("email.confirmation.emails.unequal", email => email._1 == email._2),
      "emailVerified" -> optional(text),
      "opt-in" -> optional(boolean).verifying("sa_printing_preference.opt_in_choice_required", _.isDefined).transform(
        _.map(EmailPreference.fromBoolean), (p: Option[EmailPreference]) => p.map(_.toBoolean)
      )
    )(EmailFormDataWithPreference.apply)(EmailFormDataWithPreference.unapply).verifying("error.email.optIn", b => b match {
        case EmailFormDataWithPreference((None, _), _, Some(OptIn)) => false
        case _ => true
      })
    )

  def getSubmitPreferencesView(savePrefsCall: Call)(implicit request: Request[AnyRef], withBanner: Boolean = false): Form[_] => HtmlFormat.Appendable = {
    errors => views.html.sa.prefs.sa_printing_preference(withBanner,errors, savePrefsCall)
  }

  def displayPreferencesFormAction(email: Option[Email], savePrefsCall: Call)(implicit request: Request[AnyRef]) =
    Ok(
      views.html.sa.prefs.sa_printing_preference(
      false,
        emailForm = emailFormWithPreference.fill(EmailFormDataWithPreference(email, email.map(_ => OptIn))),
        submitPrefsFormAction = savePrefsCall
      )
    )

  protected def submitEmailForm(errorsView: (Form[_]) => play.api.templates.HtmlFormat.Appendable,
                                emailWarningView: (String) => play.api.templates.HtmlFormat.Appendable,
                                successRedirect: () => Call,
                                emailConnector: EmailConnector,
                                saUtr: SaUtr,
                                savePreferences: (SaUtr, Boolean, Option[String], HeaderCarrier) => Future[Option[FormattedUri]])
                               (implicit request: Request[AnyRef]): Future[SimpleResult] = {

    implicit def hc = HeaderCarrier(request)

    emailForm.bindFromRequest()(request).fold(
      errors => Future.successful(BadRequest(errorsView(errors))),
      emailForm => {
        val emailVerificationStatus =
          if (emailForm.isEmailVerified) Future.successful(true)
          else emailConnector.validateEmailAddress(emailForm.mainEmail)

        emailVerificationStatus.flatMap {
          case true => savePreferences(saUtr, true, Some(emailForm.mainEmail), hc).map(const(Redirect(successRedirect())))
          case false => Future.successful(Ok(emailWarningView(emailForm.mainEmail)))
        }
      }
    )
  }

  protected def submitPreferencesForm(errorsView: (Form[_]) => play.api.templates.HtmlFormat.Appendable,
                                      emailWarningView: (String) => play.api.templates.HtmlFormat.Appendable,
                                      emailConnector: EmailConnector,
                                      saUtr: SaUtr,
                                      savePreferences: (SaUtr, Boolean, Option[String], HeaderCarrier) => Future[SimpleResult])
                                     (implicit request: Request[AnyRef]): Future[SimpleResult] = {

    implicit def hc = HeaderCarrier(request)

    emailFormWithPreference.bindFromRequest.fold(
      hasErrors = errors => Future.successful(BadRequest(errorsView(errors))),
      success = {
        case emailForm @ EmailFormDataWithPreference((Some(emailAddress), _), _, Some(OptIn)) =>
          val emailVerificationStatus =
            if (emailForm.isEmailVerified) Future.successful(true)
            else emailConnector.validateEmailAddress(emailAddress)

          emailVerificationStatus.flatMap {
            case true => savePreferences(saUtr, true, Some(emailAddress), hc)
            case false => Future.successful(Ok(emailWarningView(emailAddress)))
          }
        case EmailFormDataWithPreference(_, _, Some(OptOut)) =>
          savePreferences(saUtr, false, None, hc)
        case _ =>
          Future.successful(BadRequest(errorsView(emailFormWithPreference.bindFromRequest)))
      }
    )
  }

}