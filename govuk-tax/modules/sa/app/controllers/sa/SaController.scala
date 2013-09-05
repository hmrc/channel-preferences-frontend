package controllers.sa

import play.api.data._
import play.api.data.Forms._

import uk.gov.hmrc.microservice.sa.domain._
import views.html.sa._
import controllers.common._
import config.DateTimeProvider
import controllers.sa.StaticHTMLBanner._

import org.joda.time.DateTime
import controllers.common.service.FrontEndConfig
import play.api.mvc.{ Result, Request }
import controllers.common.validators.{ characterValidator, Validators }
import scala.util.{ Success, Try, Left }
import uk.gov.hmrc.microservice.sa.domain.TransactionId
import uk.gov.hmrc.microservice.sa.domain.SaRoot
import uk.gov.hmrc.common.microservice.auth.domain.Preferences
import uk.gov.hmrc.microservice.domain.User
import uk.gov.hmrc.microservice.sa.domain.SaPerson
import uk.gov.hmrc.common.microservice.auth.domain.Notification
import org.apache.commons.codec.binary.Base64
import controllers.sa.{ routes => saRoutes }

case class PrintPrefsForm(suppressPrinting: Boolean, email: Option[String], redirectUrl: String)

class SaController extends BaseController with ActionWrappers with SessionTimeoutWrapper with DateTimeProvider with Validators with CookieEncryption {

  import uk.gov.hmrc.common.microservice.auth.domain.Email.optionStringToOptionEmail

  def details = WithSessionTimeoutValidation(AuthorisedForGovernmentGatewayAction(Some(SaRegime)) { user => request => detailsAction(user, request) })

  private[sa] def detailsAction: (User, Request[_]) => Result = (user, request) => {

    val userData: SaRoot = user.regimes.sa.get

    userData.personalDetails match {
      case Some(person: SaPerson) => Ok(sa_personal_details(userData.utr, person, user.nameFromGovernmentGateway.getOrElse("")))
      case _ => NotFound //todo this should really be an error page
    }
  }

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
          case Some(sa) if sa.digital.isDefined => NoContent.withHeaders(headers)
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
        "addressLine3" -> optional(text
          .verifying("error.sa.address.optionallines.maxlengthviolation", isOptionalAddressLineLengthValid)
          .verifying("error.sa.address.invalidcharacter", characterValidator.containsValidAddressCharacters _)),
        "addressLine4" -> optional(text
          .verifying("error.sa.address.optionallines.maxlengthviolation", isOptionalAddressLineLengthValid)
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

  def changeAddress = WithSessionTimeoutValidation(AuthorisedForGovernmentGatewayAction(Some(SaRegime)) { user => request => changeAddressAction(user, request) })

  private[sa] def changeAddressAction: (User, Request[_]) => Result = (user, request) => {
    Ok(sa_personal_details_update(changeAddressForm))
  }

  def redisplayChangeAddress() = WithSessionTimeoutValidation(AuthorisedForGovernmentGatewayAction(Some(SaRegime)) { user => request => redisplayChangeAddressAction(user, request) })

  private[sa] def redisplayChangeAddressAction: (User, Request[_]) => Result = (user, request) => {
    val form = changeAddressForm.bindFromRequest()(request)
    Ok(sa_personal_details_update(form))
  }

  def submitChangeAddress() = WithSessionTimeoutValidation(AuthorisedForGovernmentGatewayAction(Some(SaRegime)) { user => request => submitChangeAddressAction(user, request) })

  private[sa] def submitChangeAddressAction: (User, Request[_]) => Result = (user, request) => {
    changeAddressForm.bindFromRequest()(request).fold(
      errors => BadRequest(sa_personal_details_update(errors)),
      formData => {
        Ok(sa_personal_details_confirmation(changeAddressForm, formData))
      }
    )
  }

  def confirmChangeAddress = WithSessionTimeoutValidation(AuthorisedForGovernmentGatewayAction(Some(SaRegime)) { user => request => confirmChangeAddressAction(user, request) })

  private[sa] def confirmChangeAddressAction: (User, Request[_]) => Result = (user, request) => {
    changeAddressForm.bindFromRequest()(request).fold(
      errors => BadRequest(sa_personal_details_update(errors)),
      formData => {
        user.regimes.sa.get.updateIndividualMainAddress(formData.toUpdateAddress) match {
          case Left(errorMessage: String) => Redirect(saRoutes.SaController.changeAddressFailed(encryptParameter(errorMessage)))
          case Right(transactionId: TransactionId) => Redirect(saRoutes.SaController.changeAddressComplete(encryptParameter(transactionId.oid)))
        }
      }
    )
  }

  private def encryptParameter(value: String): String = SecureParameter(value, now()).encrypt

  private def decryptParameter(value: String): Try[SecureParameter] = SecureParameter.decrypt(value)

  def changeAddressComplete(id: String) = WithSessionTimeoutValidation(AuthorisedForGovernmentGatewayAction(Some(SaRegime)) { user => request => changeAddressCompleteAction(id) })

  private[sa] def changeAddressCompleteAction(id: String) = {

    decryptParameter(id) match {
      case Success(SecureParameter(transactionId, _)) => Ok(sa_personal_details_confirmation_receipt(TransactionId(transactionId)))
      case _ => NotFound
    }
  }

  def changeAddressFailed(id: String) = WithSessionTimeoutValidation(AuthorisedForGovernmentGatewayAction(Some(SaRegime)) { user => request => changeAddressFailedAction(id) })

  private[sa] def changeAddressFailedAction(id: String) = {

    decryptParameter(id) match {
      case Success(SecureParameter(errorMessage, _)) => Ok(sa_personal_details_update_failed(errorMessage))
      case _ => NotFound
    }
  }
}

