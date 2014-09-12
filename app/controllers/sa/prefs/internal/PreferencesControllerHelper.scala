package controllers.sa.prefs.internal

import connectors.EmailConnector
import controllers.sa.prefs._
import controllers.sa.prefs.internal.EmailOptInCohorts.Cohort
import play.api.data.Forms._
import play.api.data._
import play.api.mvc.Results._
import play.api.mvc.{Call, Request, Result}
import play.api.templates.HtmlFormat
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.play.connectors.HeaderCarrier
import uk.gov.hmrc.play.logging.MdcLoggingExecutionContext._

import scala.Function.const
import scala.concurrent._

trait PreferencesControllerHelper {

  val emailWithLimitedLength: Mapping[String] =
    text
      .verifying("error.email", EmailAddress.isValid _)
      .verifying("error.email_too_long", email => email.size < 320)

  // TODO the duplication of these forms is all wrong - need to alter the field names in the HTML to
  // be able to restructure and sort this out.
  protected val emailForm = Form[EmailFormData](mapping(
    "email" -> tuple(
      "main" -> emailWithLimitedLength,
      "confirm" -> optional(text)
    ).verifying("email.confirmation.emails.unequal", email => email._1 == email._2.getOrElse("")),
    "emailVerified" -> optional(text)
  )(EmailFormData.apply)(EmailFormData.unapply))

  protected val preferenceForm =
    Form[PreferenceData](mapping(
      "opt-in" -> optional(boolean).verifying("sa_printing_preference.opt_in_choice_required", _.isDefined)
    )(PreferenceData.apply)(PreferenceData.unapply))

  protected val emailFormWithPreference =
    Form[EmailFormDataWithPreference](mapping(
      "email" -> tuple(
        "main" -> optional(emailWithLimitedLength),
        "confirm" -> optional(text)
      ),
      "emailVerified" -> optional(text),
      "opt-in" -> optional(boolean).verifying("sa_printing_preference.opt_in_choice_required", _.isDefined).transform(
        _.map(EmailPreference.fromBoolean), (p: Option[EmailPreference]) => p.map(_.toBoolean)
      )
    )(EmailFormDataWithPreference.apply)(EmailFormDataWithPreference.unapply)
      .verifying("error.email.optIn", b => b match {
        case EmailFormDataWithPreference((None, _), _, Some(OptIn)) => false
        case _ => true
      })
      .verifying("email.confirmation.emails.unequal", formData => formData.email._1 == formData.email._2)
    )

  def getSubmitPreferencesView(savePrefsCall: Call, cohort: Cohort)(implicit request: Request[AnyRef], withBanner: Boolean = false): Form[_] => HtmlFormat.Appendable = {
    errors => views.html.sa.prefs.sa_printing_preference(withBanner, errors, savePrefsCall, cohort)
  }

  def displayPreferencesFormAction(email: Option[EmailAddress], savePrefsCall: Call, withBanner: Boolean = false, cohort: Cohort)(implicit request: Request[AnyRef]) =
    Ok(
    views.html.sa.prefs.sa_printing_preference(
      withBanner,
      emailForm = emailFormWithPreference.fill(EmailFormDataWithPreference(email, email.map(_ => OptIn))),
      submitPrefsFormAction = savePrefsCall,
      cohort
    )
  )

  protected def submitEmailForm(errorsView: (Form[_]) => play.api.templates.HtmlFormat.Appendable,
                                emailWarningView: (String) => play.api.templates.HtmlFormat.Appendable,
                                successRedirect: () => Call,
                                emailConnector: EmailConnector,
                                saUtr: SaUtr,
                                cohort: Cohort,
                                savePreferences: (SaUtr, Boolean, Option[String], Cohort, HeaderCarrier) => Future[_])
                               (implicit request: Request[AnyRef]): Future[Result] = {

    implicit def hc = HeaderCarrier.fromSessionAndHeaders(request.session, request.headers)

    emailForm.bindFromRequest()(request).fold(
      errors => Future.successful(BadRequest(errorsView(errors))),
      emailForm => {
        val emailVerificationStatus =
          if (emailForm.isEmailVerified) Future.successful(true)
          else emailConnector.isValid(emailForm.mainEmail)

        emailVerificationStatus.flatMap {
          case true => savePreferences(
            saUtr,
            true,
            Some(emailForm.mainEmail),
            cohort,
            hc).map(const(Redirect(successRedirect())))
          case false => Future.successful(Ok(emailWarningView(emailForm.mainEmail)))
        }
      }
    )
  }

  protected def submitPreferencesForm(errorsView: (Form[_]) => play.api.templates.HtmlFormat.Appendable,
                                      emailWarningView: (String) => play.api.templates.HtmlFormat.Appendable,
                                      emailConnector: EmailConnector,
                                      saUtr: SaUtr,
                                      savePreferences: (SaUtr, Boolean, Option[String], HeaderCarrier) => Future[Result])
                                     (implicit request: Request[AnyRef]): Future[Result] = {

    implicit def hc = HeaderCarrier.fromSessionAndHeaders(request.session, request.headers)

    preferenceForm.bindFromRequest.fold(
      errors => Future.successful(BadRequest(errorsView(errors))),
      success => {
        if (success.optedIn.exists(_ == false)) savePreferences(saUtr, false, None, hc)
        else {
          emailFormWithPreference.bindFromRequest.fold(
            errors => Future.successful(BadRequest(errorsView(errors))),
            success = {
              case emailForm@EmailFormDataWithPreference((Some(emailAddress), _), _, Some(OptIn)) =>
                val emailVerificationStatus =
                  if (emailForm.isEmailVerified) Future.successful(true)
                  else emailConnector.isValid(emailAddress)

                emailVerificationStatus.flatMap {
                  case true => savePreferences(saUtr, true, Some(emailAddress), hc)
                  case false => Future.successful(Ok(emailWarningView(emailAddress)))
                }
              case _ =>
                Future.successful(BadRequest(errorsView(emailFormWithPreference.bindFromRequest)))
            }
          )
        }
      }
    )
  }

}
