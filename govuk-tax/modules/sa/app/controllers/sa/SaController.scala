package controllers.sa

import play.api.data._
import play.api.data.Forms._

import uk.gov.hmrc.microservice.sa.domain._
import views.html.sa._
import controllers.common.{ SsoPayloadEncryptor, SessionTimeoutWrapper, ActionWrappers, BaseController }
import play.api.libs.json.Json
import config.DateTimeProvider
import controllers.sa.StaticHTMLBanner._

import org.joda.time.DateTime
import controllers.common.service.FrontEndConfig
import uk.gov.hmrc.microservice.sa.domain.SaRoot
import uk.gov.hmrc.common.microservice.auth.domain.Preferences
import scala.Some
import uk.gov.hmrc.microservice.sa.domain.SaPerson
import uk.gov.hmrc.common.microservice.auth.domain.SaPreferences
import play.api.mvc.{ Result, Request }
import uk.gov.hmrc.microservice.domain.User
import controllers.common.validators.{ characterValidator, Validators }
import scala.util.Left

case class PrintPrefsForm(suppressPrinting: Boolean, email: Option[String], redirectUrl: String)

class SaController extends BaseController with ActionWrappers with SessionTimeoutWrapper with DateTimeProvider with Validators {

  val printPrefsForm: Form[PrintPrefsForm] = Form(
    mapping(

      "prefs" -> tuple(
        "suppressPrinting" -> boolean,
        "email" -> optional(email)).verifying("error.prefs.email.missing", printPrefs =>
          !printPrefs._1 || (printPrefs._1 && printPrefs._2.isDefined)
        ),
      "redirectUrl" -> text) {
        (prefs, redirectUrl) => PrintPrefsForm(prefs._1, prefs._2, redirectUrl)
      } {
        form => Some((form.suppressPrinting, form.email), form.redirectUrl)
      }
  )

  def details = WithSessionTimeoutValidation(AuthorisedForGovernmentGatewayAction(Some(SaRegime)) { user => request => detailsAction(user, request) })

  private[sa] def detailsAction: (User, Request[_]) => Result = (user, request) => {
    val userData: SaRoot = user.regimes.sa.get

    userData.personalDetails match {
      case Some(person: SaPerson) => Ok(sa_personal_details(userData.utr, person, user.nameFromGovernmentGateway.getOrElse("")))
      case _ => NotFound //todo this should really be an error page
    }
  }

  def checkPrintPreferences(encryptedJson: String) = UnauthorisedAction { request => checkPrintPreferencesAction(request, encryptedJson) }

  private[sa] def checkPrintPreferencesAction: (Request[_], String) => Result = (request, encryptedQueryParameters) => {
    val decryptedQueryParameters = SsoPayloadEncryptor.decrypt(encryptedQueryParameters)
    val splitQueryParams = decryptedQueryParameters.split(":")
    val utr = splitQueryParams(0).trim
    val time = splitQueryParams(1).trim.toLong

    val currentTime: DateTime = now()
    if (currentTime.minusMinutes(5).isAfter(time)) BadRequest
    else {
      val headers = ("Access-Control-Allow-Origin", "*")
      authMicroService.preferences(utr) match {
        case None => NoContent
        case Some(pref) => pref.sa match {
          case Some(sa) if sa.digitalNotifications.isDefined => NoContent.withHeaders(headers)
          case _ => Ok(saPreferences(s"${FrontEndConfig.frontendUrl}/sa/prefs")).withHeaders(headers)
        }
      }
    }
  }

  def prefsForm = WithSessionTimeoutValidation(AuthorisedForGovernmentGatewayAction(Some(SaRegime)) { user => request => prefsFormAction(user, request) })

  private[sa] def prefsFormAction: (User, Request[_]) => Result = (user, request) => {
    request.queryString.get("rd") match {
      case Some(rd) => Ok(sa_prefs_details(printPrefsForm.fill(PrintPrefsForm(true, None, rd(0)))))
      case _ => NotFound
    }
  }

  def submitPrefsForm() = WithSessionTimeoutValidation(AuthorisedForGovernmentGatewayAction(Some(SaRegime)) { user => request => submitPrefsFormAction(user, request) })

  private[sa] def submitPrefsFormAction: (User, Request[_]) => Result = (user, request) => {
    printPrefsForm.bindFromRequest()(request).fold(
      errors => BadRequest(sa_prefs_details(errors)),
      printPrefsForm => {
        val authResponse = authMicroService.savePreferences(user.user, Preferences(sa = Some(SaPreferences(Some(printPrefsForm.suppressPrinting), printPrefsForm.email))))
        authResponse match {
          case Some(_) => Redirect(printPrefsForm.redirectUrl)
          case _ => NotFound //todo this should really be an error page
        }
      }
    )
  }

  val changeAddressForm: Form[ChangeAddressForm] = Form(
    mapping(
      "addressLine1" -> text
        .verifying("error.sa.address.line1.mandatory", notBlank _)
        .verifying("error.sa.address.mainlines.maxlengthviolation", isMainAddressLineLengthValid)
        .verifying("error.sa.address.invalidcharacter", characterValidator.containsValidAddressCharacters _),
      "addressLine2" -> text
        .verifying("error.sa.address.line2.mandatory", notBlank _)
        .verifying("error.sa.address.mainlines.maxlengthviolation", isMainAddressLineLengthValid)
        .verifying("error.sa.address.invalidcharacter", characterValidator.containsValidAddressCharacters _),
      "optionalAddressLines" -> tuple(
        "addressLine3" -> optional(text.verifying("error.sa.address.optionallines.maxlengthviolation", isOptionalAddressLineLengthValid)
          .verifying("error.sa.address.invalidcharacter", characterValidator.containsValidAddressCharacters _)),
        "addressLine4" -> optional(text.verifying("error.sa.address.optionallines.maxlengthviolation", isOptionalAddressLineLengthValid)
          .verifying("error.sa.address.invalidcharacter", characterValidator.containsValidAddressCharacters _))
      ).verifying("error.sa.address.line3.mandatory", optionalLines => isBlank(optionalLines._2.getOrElse("")) || (notBlank(optionalLines._1.getOrElse("")) && notBlank(optionalLines._2.getOrElse("")))),
      "postcode" -> text
        .verifying("error.sa.postcode.mandatory", notBlank _)
        .verifying("error.sa.postcode.lengthviolation", isPostcodeLengthValid _)
        .verifying("error.sa.postcode.invalidcharacter", characterValidator.containsValidPostCodeCharacters _),
      "additionalDeliveryInformation" -> optional(text)) {
        (addressLine1, addressLine2, optionalAddressLines, postcode, additionalDeliveryInformation) =>
          ChangeAddressForm(Some(addressLine1), Some(addressLine2), optionalAddressLines._1, optionalAddressLines._2, Some(postcode), additionalDeliveryInformation)
      } {
        form => Some((form.addressLine1.getOrElse(""), form.addressLine2.getOrElse(""), (form.addressLine3, form.addressLine4), form.postcode.get, form.additionalDeliveryInformation))
      }

  )

  def changeMyAddressForm = WithSessionTimeoutValidation(AuthorisedForGovernmentGatewayAction(Some(SaRegime)) { user => request => changeMyAddressFormAction(user, request) })

  private[sa] def changeMyAddressFormAction: (User, Request[_]) => Result = (user, request) => {
    Ok(sa_personal_details_update(changeAddressForm))
  }

  def redisplayChangeAddressForm() = WithSessionTimeoutValidation(AuthorisedForGovernmentGatewayAction(Some(SaRegime)) { user => request => redisplayChangeAddressFormAction(user, request) })

  private[sa] def redisplayChangeAddressFormAction: (User, Request[_]) => Result = (user, request) => {
    val form = changeAddressForm.bindFromRequest()(request)
    Ok(sa_personal_details_update(form))
  }

  def submitChangeAddressForm() = WithSessionTimeoutValidation(AuthorisedForGovernmentGatewayAction(Some(SaRegime)) { user => request => submitChangeAddressFormAction(user, request) })

  private[sa] def submitChangeAddressFormAction: (User, Request[_]) => Result = (user, request) => {
    changeAddressForm.bindFromRequest()(request).fold(
      errors => BadRequest(sa_personal_details_update(errors)),
      formData => {
        Ok(sa_personal_details_confirmation(changeAddressForm, formData))
      }
    )
  }

  def submitConfirmChangeMyAddressForm = WithSessionTimeoutValidation(AuthorisedForGovernmentGatewayAction(Some(SaRegime)) { user => request => submitConfirmChangeMyAddressFormAction(user, request) })

  private[sa] def submitConfirmChangeMyAddressFormAction: (User, Request[_]) => Result = (user, request) => {
    changeAddressForm.bindFromRequest()(request).fold(
      errors => BadRequest(sa_personal_details_update(errors)),
      formData => {
        val uri = s"/sa/individual/${user.userAuthority.utr.get}/main-address"

        // TODO [JJS] This should do a redirect to the confirmation (POST-REDIRECT-GET) - perhaps to a generic "transaction confirmation page" reading from the transaction service?

        saMicroService.updateMainAddress(uri, formData.toUpdateAddress) match {
          case Left(errorMessage: String) => Ok(sa_personal_details_update_failed(errorMessage))
          case Right(transactionId: TransactionId) => Ok(sa_personal_details_confirmation_receipt(transactionId))
        }
      }
    )
  }
}