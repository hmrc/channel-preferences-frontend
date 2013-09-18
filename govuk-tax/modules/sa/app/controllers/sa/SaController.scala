package controllers.sa

import play.api.data._
import play.api.data.Forms._

import uk.gov.hmrc.common.microservice.sa.domain._
import views.html.sa._
import controllers.common._
import config.DateTimeProvider

import play.api.mvc.{ Result, Request }
import controllers.common.validators.{ characterValidator, Validators }
import scala.util.{ Success, Try, Left }
import uk.gov.hmrc.common.microservice.sa.domain.TransactionId
import uk.gov.hmrc.common.microservice.sa.domain.SaRoot
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.common.microservice.sa.domain.SaPerson
import controllers.sa.{ routes => saRoutes }

class SaController extends BaseController with ActionWrappers with SessionTimeoutWrapper with DateTimeProvider with Validators {

  def details = WithSessionTimeoutValidation(AuthorisedForGovernmentGatewayAction(Some(SaRegime)) { user => request => detailsAction(user, request) })

  private[sa] def detailsAction: (User, Request[_]) => Result = (user, request) => {

    val userData: SaRoot = user.regimes.sa.get

    userData.personalDetails match {
      case Some(person: SaPerson) => Ok(sa_personal_details(userData.utr, person, user.nameFromGovernmentGateway.getOrElse("")))
      case _ => NotFound //FIXME: this should really be an error page
    }
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

