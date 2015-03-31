package controllers.sa.prefs.internal

import connectors.EmailConnector
import controllers.sa.prefs._
import play.api.data.Forms._
import play.api.data._
import play.api.mvc.Results._
import play.api.mvc.{Call, Request, Result}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

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

  protected val optInOrOutForm =
    Form[PreferenceData](mapping(
      "opt-in" -> optional(boolean).verifying("sa_printing_preference.opt_in_choice_required", _.isDefined)
    )(PreferenceData.apply)(PreferenceData.unapply))

  protected val optInDetailsForm =
    Form[EmailFormDataWithPreference](mapping(
      "email" -> tuple(
        "main" -> optional(emailWithLimitedLength),
        "confirm" -> optional(text)
      ),
      "emailVerified" -> optional(text),
      "opt-in" -> optional(boolean).verifying("sa_printing_preference.opt_in_choice_required", _.isDefined).transform(
        _.map(EmailPreference.fromBoolean), (p: Option[EmailPreference]) => p.map(_.toBoolean)
      ),
      "accept-tc" -> optional(boolean).verifying("sa_printing_preference.accept_tc_required", _.contains(true))
    )(EmailFormDataWithPreference.apply)(EmailFormDataWithPreference.unapply)
      .verifying("error.email.optIn", _ match {
      case EmailFormDataWithPreference((None, _), _, Some(OptIn), _) => false
      case _ => true
    })
      .verifying("email.confirmation.emails.unequal", formData => formData.email._1 == formData.email._2)
    )

  def getSubmitPreferencesView(savePrefsCall: Call, cohort: OptInCohort)(implicit request: Request[AnyRef], withBanner: Boolean = false): Form[_] => HtmlFormat.Appendable = {
    errors => views.html.sa.prefs.sa_printing_preference(withBanner, errors, savePrefsCall, cohort)
  }

  def displayPreferencesFormAction(email: Option[EmailAddress], savePrefsCall: Call, withBanner: Boolean = false, cohort: OptInCohort)(implicit request: Request[AnyRef]) =
    Ok(
      views.html.sa.prefs.sa_printing_preference(
        withBanner,
        emailForm = optInDetailsForm.fill(EmailFormDataWithPreference(email, email.map(_ => OptIn), Some(false))),
        submitPrefsFormAction = savePrefsCall,
        cohort
      )
    )

  protected def submitEmailForm(errorsView: (Form[_]) => HtmlFormat.Appendable,
                                emailWarningView: (String) => HtmlFormat.Appendable,
                                successRedirect: () => Call,
                                emailConnector: EmailConnector,
                                saUtr: SaUtr,
                                savePreferences: (SaUtr, Boolean, Option[String], HeaderCarrier) => Future[_])
                               (implicit request: Request[AnyRef]): Future[Result] = {

    implicit def hc: HeaderCarrier = HeaderCarrier.fromSessionAndHeaders(request.session, request.headers)

    emailForm.bindFromRequest()(request).fold(
      errors => Future.successful(BadRequest(errorsView(errors))),
      emailForm => {
        val emailVerificationStatus =
          if (emailForm.isEmailVerified) Future.successful(true)
          else emailConnector.isValid(emailForm.mainEmail)

        emailVerificationStatus.flatMap {
          case true => savePreferences(saUtr, true, Some(emailForm.mainEmail), hc).map(const(Redirect(successRedirect())))
          case false => Future.successful(Ok(emailWarningView(emailForm.mainEmail)))
        }
      }
    )
  }

  protected def submitPreferencesForm(errorsView: (Form[_]) => HtmlFormat.Appendable,
                                      emailWarningView: (String) => HtmlFormat.Appendable,
                                      emailConnector: EmailConnector,
                                      saUtr: SaUtr,
                                      savePreferences: (SaUtr, Boolean, Option[String], Boolean, HeaderCarrier) => Future[Result])
                                     (implicit request: Request[AnyRef]): Future[Result] = {

    implicit def hc: HeaderCarrier = HeaderCarrier.fromSessionAndHeaders(request.session, request.headers)

    optInOrOutForm.bindFromRequest.fold(
      sadForm => Future.successful(BadRequest(errorsView(sadForm))),
      happyForm => {
        if (happyForm.optedIn.contains(false)) savePreferences(saUtr, false, None, false, hc)
        else {
          optInDetailsForm.bindFromRequest.fold(
            errors => Future.successful(BadRequest(errorsView(errors))),
            success = {
              case emailForm@EmailFormDataWithPreference((Some(emailAddress), _), _, Some(OptIn), Some(true)) =>
                val emailVerificationStatus =
                  if (emailForm.isEmailVerified) Future.successful(true)
                  else emailConnector.isValid(emailAddress)

                emailVerificationStatus.flatMap {
                  case true => savePreferences(saUtr, true, Some(emailAddress), emailForm.acceptedTCs.contains(true), hc)
                  case false => Future.successful(Ok(emailWarningView(emailAddress)))
                }
              case _ =>
                Future.successful(BadRequest(errorsView(optInDetailsForm.bindFromRequest)))
            }
          )
        }
      }
    )
  }

}
