package controllers.sa

import views.html.sa._

import play.api.data._
import play.api.data.Forms._

import controllers.common._
import config.DateTimeProvider

import play.api.mvc.{Request, Result}
import controllers.common.validators.{characterValidator, Validators}
import scala.util.{Success, Try, Left}
import uk.gov.hmrc.common.microservice.sa.domain.write.TransactionId
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.common.microservice.sa.domain.SaDomain.SaPerson
import controllers.sa.{routes => saRoutes}
import uk.gov.hmrc.common.microservice.sa.domain.SaRegime

class SaController
  extends BaseController
  with ActionWrappers
  with DateTimeProvider
  with Validators {

  def details = AuthorisedForGovernmentGatewayAction(Some(SaRegime)) {
    implicit user =>
      implicit request =>
        detailsAction
  }

  def changeAddress = AuthorisedForGovernmentGatewayAction(Some(SaRegime)) {
    implicit user =>
      implicit request =>
        changeAddressAction
  }

  def redisplayChangeAddress() = AuthorisedForGovernmentGatewayAction(Some(SaRegime)) {
    implicit user =>
      implicit request =>
        redisplayChangeAddressAction
  }

  def submitChangeAddress() = AuthorisedForGovernmentGatewayAction(Some(SaRegime)) {
    implicit user => implicit request => submitChangeAddressAction
  }

  def confirmChangeAddress = AuthorisedForGovernmentGatewayAction(Some(SaRegime)) {
    implicit user =>
      implicit request =>
        confirmChangeAddressAction
  }

  def changeAddressComplete(id: String) = AuthorisedForGovernmentGatewayAction(Some(SaRegime)) {
    implicit user =>
      implicit request =>
        changeAddressCompleteAction(id)
  }

  private[sa] def detailsAction(implicit user: User, request: Request[_]): Result = {
    val userData = user.regimes.sa.get.get

    userData.personalDetails match {
      case Some(person: SaPerson) => Ok(sa_personal_details(userData.utr.utr, person, user.nameFromGovernmentGateway.getOrElse("")))
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

  private[sa] def changeAddressAction(implicit user: User, request: Request[_]): Result =
    Ok(sa_personal_details_update(changeAddressForm = changeAddressForm))

  private[sa] def redisplayChangeAddressAction(implicit user: User, request: Request[_]): Result =
    Ok(sa_personal_details_update(changeAddressForm = changeAddressForm.bindFromRequest()(request)))

  private[sa] def submitChangeAddressAction(implicit user: User, request: Request[_]): Result =
    changeAddressForm.bindFromRequest()(request).fold(
      errors =>
        BadRequest(sa_personal_details_update(errors)),
      formData =>
        Ok(sa_personal_details_confirmation(
          changeAddressForm = changeAddressForm,
          changeAddressFormData = formData))
    )

  private[sa] def confirmChangeAddressAction(implicit user: User, request: Request[_]): Result =
    changeAddressForm.bindFromRequest()(request).fold(
      errors =>
        BadRequest(sa_personal_details_update(errors)),
      formData =>
        user.regimes.sa.get.get.updateIndividualMainAddress(formData.toUpdateAddress) match {
          case Left(errorMessage: String) => Redirect(saRoutes.SaController.changeAddressFailed(encryptParameter(errorMessage)))
          case Right(transactionId: TransactionId) => Redirect(saRoutes.SaController.changeAddressComplete(encryptParameter(transactionId.oid)))
        }
    )

  private def encryptParameter(value: String): String = SecureParameter(value, now()).encrypt

  private def decryptParameter(value: String): Try[SecureParameter] = SecureParameter.decrypt(value)

  private[sa] def changeAddressCompleteAction(id: String)(implicit user: User, request: Request[_]): Result =
    decryptParameter(id) match {
      case Success(SecureParameter(transactionId, _)) => Ok(sa_personal_details_confirmation_receipt(TransactionId(transactionId)))
      case _ => NotFound
    }

  def changeAddressFailed(id: String) = AuthorisedForGovernmentGatewayAction(Some(SaRegime)) {
    implicit user => implicit request => changeAddressFailedAction(id)
  }

  private[sa] def changeAddressFailedAction(id: String)(implicit user: User, request: Request[_]): Result =
    decryptParameter(id) match {
      case Success(SecureParameter(errorMessage, _)) => Ok(sa_personal_details_update_failed(errorMessage))
      case _ => NotFound
    }
}

