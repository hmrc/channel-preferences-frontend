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

  private val emailMainAndConfirmMapping: Mapping[(String, Option[String])] = tuple(
    "main" -> email.verifying("error.email_too_long", email => email.size < 320),
    "confirm" -> optional(text)
  ).verifying("email.confirmation.emails.unequal", email => email._1 == email._2.getOrElse(""))

  protected val emailForm = Form[EmailFormData](mapping(
    "email" -> emailMainAndConfirmMapping,
    "emailVerified" -> optional(text)
  )(EmailFormData.apply)(EmailFormData.unapply))

  protected val emailFormWithPreference = {
    Form[EmailFormDataWithPreference](mapping(
      "email" -> emailMainAndConfirmMapping,
      "emailVerified" -> optional(text),
      "opt-in" -> optional(boolean).verifying("sa_printing_preference.opt_in_choice_required", _.isDefined).transform(
        EmailPreference.fromBoolean, (p: EmailPreference) => p.toBoolean
      )
    )(EmailFormDataWithPreference.apply)(EmailFormDataWithPreference.unapply))
  }

  def getSubmitPreferencesView(savePrefsCall: Call, keepPaperCall: Call)(implicit request: Request[AnyRef]): Form[_] => HtmlFormat.Appendable = {
    errors => views.html.sa.prefs.sa_printing_preference(errors, savePrefsCall, keepPaperCall)
  }

  def displayPreferencesForm(email: Option[Email], savePrefsCall: Call, keepPaperCall: Call)(implicit request: Request[AnyRef]) = {
    Ok(views.html.sa.prefs.sa_printing_preference(
      emailForm = emailForm.fill(EmailFormData(email)),
      submitPrefsFormAction = savePrefsCall,
      submitKeepPaperAction = keepPaperCall))
  }

  protected def submitEmailForm(errorsView: (Form[_]) => play.api.templates.HtmlFormat.Appendable,
                                emailWarningView: (String) => play.api.templates.HtmlFormat.Appendable,
                                successRedirect: () => Call,
                                emailConnector: EmailConnector,
                                saUtr: SaUtr,
                                savePreferences: (SaUtr, Boolean, Option[String], HeaderCarrier) => Future[Option[FormattedUri]])
                               (implicit request: Request[AnyRef]): Future[SimpleResult] = {
    emailForm.bindFromRequest()(request).fold(
      errors => Future.successful(BadRequest(errorsView(errors))),
      emailForm => verifyAndSaveEmail(emailForm, emailConnector, savePreferences, saUtr, successRedirect, emailWarningView)
    )
  }
  def verifyAndSaveEmail(emailForm: EmailFormData, emailConnector: EmailConnector, savePreferences: (SaUtr, Boolean, Option[String], HeaderCarrier) => Future[Option[FormattedUri]], saUtr: SaUtr, successRedirect: () => Call, emailWarningView: (String) => HtmlFormat.Appendable): Future[SimpleResult] = {
    implicit def hc = HeaderCarrier(request)

    val emailVerificationStatus =
      if (emailForm.isEmailVerified) Future.successful(true)
      else emailConnector.validateEmailAddress(emailForm.mainEmail)

    emailVerificationStatus.flatMap {
      case true => savePreferences(saUtr, true, Some(emailForm.mainEmail), hc()).map(const(Redirect(successRedirect())))
      case false => Future.successful(Ok(emailWarningView(emailForm.mainEmail)))
    }
  }

  protected def submitPreferencesForm(errorsView: (Form[_]) => play.api.templates.HtmlFormat.Appendable,
                                      emailWarningView: (String) => play.api.templates.HtmlFormat.Appendable,
                                      successRedirect: () => Call,
                                      emailConnector: EmailConnector,
                                      saUtr: SaUtr,
                                      savePreferences: (SaUtr, Boolean, Option[String], HeaderCarrier) => Future[Option[FormattedUri]])
                                     (implicit request: Request[AnyRef]): Future[SimpleResult] = {

    implicit def hc = HeaderCarrier(request)

    emailFormWithPreference.bindFromRequest.fold(
      hasErrors = errors => Future.successful(BadRequest(errorsView(errors))),
      success = {
        case EmailFormDataWithPreference(_, _, OptIn) =>
            val emailVerificationStatus =
              if (emailForm.isEmailVerified) Future.successful(true)
              else emailConnector.validateEmailAddress(emailForm.mainEmail)

            emailVerificationStatus.flatMap {
              case true => savePreferences(saUtr, true, Some(emailForm.mainEmail), hc).map(const(Redirect(successRedirect())))
              case false => Future.successful(Ok(emailWarningView(emailForm.mainEmail)))
            }
          }
        case EmailFormDataWithPreference(_, _, OptOut) =>
          //TODO
          Future.successful(Ok)
      }
    )
  }

}