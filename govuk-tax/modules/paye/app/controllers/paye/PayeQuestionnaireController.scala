package controllers.paye

import controllers.common.BaseController
import controllers.common.actions.{HeaderCarrier, Actions}
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import uk.gov.hmrc.common.microservice.audit.AuditConnector
import controllers.common.service.Connectors
import uk.gov.hmrc.common.microservice.paye.domain.PayeRegime
import play.api.mvc.{AnyContent, Request}
import PayeQuestionnaireUtils._
import views.html.paye._
import models.paye._
import uk.gov.hmrc.common.microservice.audit.AuditEvent
import scala.Some
import play.api.mvc.SimpleResult
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.common.microservice.paye.domain.AddBenefitResponse
import uk.gov.hmrc.common.microservice.paye.domain.TransactionId


class PayeQuestionnaireController(override val auditConnector: AuditConnector, override val authConnector: AuthConnector)
  extends BaseController with Actions with PayeRegimeRoots {

  def this() = this(Connectors.auditConnector, Connectors.authConnector)

  def submitQuestionnaire = AuthorisedFor(PayeRegime) {
    implicit user =>
      implicit request =>
        submitQuestionnaireAction
  }

  private[paye] def submitQuestionnaireAction(implicit request: Request[_], user: User): SimpleResult = {
    payeQuestionnaireForm.bindFromRequest().fold(
      errors => BadRequest,
      (formData: PayeQuestionnaireFormData) => {
        audit(buildAuditEvent(formData))
        forwardToConfirmationPage(formData.journeyType, formData.transactionId, formData.oldTaxCode, formData.newTaxCode)
      }
    )
  }


  private[paye] def buildAuditEvent(formData: PayeQuestionnaireFormData): AuditEvent = {
    AuditEvent(
      auditType = "questionnaire",
      tags = Map("questionnaire-transactionId" -> formData.transactionId),
      detail = payeQuestionnaireFormDataToMap(formData)
    )
  }

  private def payeQuestionnaireFormDataToMap(formData: PayeQuestionnaireFormData) = {
    val paramsToFilterOut = Set("transactionId", "journeyType", "oldTaxCode", "newTaxCode")
    formData.getClass.getDeclaredFields.filter(field => !paramsToFilterOut.contains(field.getName)).map {
      field =>
        field.setAccessible(true)
        field.getName -> (field.get(formData) match {
          case Some(x) => x.toString
          case x => x.toString
        })
    }.toMap[String, String]
  }

  private[paye] def forwardToConfirmationPage(journeyType: Option[String], transactionId: String, oldTaxCode: Option[String], newTaxCode: Option[String])(implicit request: Request[_], user: User): SimpleResult = {
    import controllers.paye.BenefitUpdateConfirmationBuilder._

    val allParamsAreDefined = Seq(journeyType, oldTaxCode, newTaxCode).filter(!_.isDefined).isEmpty
    if (!allParamsAreDefined)
      Redirect(routes.PayeHomeController.home(None))
    else {
      val addBenefitResponse = AddBenefitResponse(TransactionId(transactionId), newTaxCode, None)
      val benefitUpdatedConfirmationData = buildBenefitUpdatedConfirmationData(oldTaxCode.get, addBenefitResponse)
      try {
        toJourneyType(journeyType.get) match {
          case jType@(AddCar | AddFuel) => Ok(add_car_benefit_confirmation(benefitUpdatedConfirmationData, jType, showQuestionnaire = false))
          case jType@(RemoveCar | RemoveFuel | RemoveCarAndFuel) => Ok(remove_benefit_confirmation(getBenefitType(jType), benefitUpdatedConfirmationData, showQuestionnaire = false))
          case ReplaceCar => Ok(replace_benefit_confirmation(transactionId, oldTaxCode.get, newTaxCode, showQuestionnaire = false))
        }
      }
      catch {
        case e: IllegalJourneyTypeException => Redirect(routes.PayeHomeController.home(None))
      }
    }

  }

  private[paye] def audit(auditEvent: AuditEvent)(implicit request: Request[_]): Unit = {
    implicit val hc = HeaderCarrier(request)
    auditConnector.audit(auditEvent)
  }
}
