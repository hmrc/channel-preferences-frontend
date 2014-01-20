package controllers.paye

import controllers.common.BaseController
import controllers.common.actions.{HeaderCarrier, Actions}
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import uk.gov.hmrc.common.microservice.audit.AuditConnector
import controllers.common.service.Connectors
import uk.gov.hmrc.common.microservice.paye.domain.PayeRegime
import uk.gov.hmrc.common.microservice.audit.AuditEvent
import play.api.mvc.{Request, SimpleResult}
import PayeQuestionnaireUtils._


class PayeQuestionnaireController(override val auditConnector: AuditConnector, override val authConnector: AuthConnector)
  extends BaseController with Actions with PayeRegimeRoots {

  def this() = this(Connectors.auditConnector, Connectors.authConnector)

  def submitQuestionnaire = AuthorisedFor(PayeRegime) {
    user =>
      implicit request =>
        submitQuestionnaireAction
  }

  private[paye] def submitQuestionnaireAction(implicit request: Request[_]): SimpleResult = {
    payeQuestionnaireForm.bindFromRequest().fold(
      errors => BadRequest,
      (formData: PayeQuestionnaireFormData) => {
        audit(buildAuditEvent(formData))
        Ok
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
    formData.getClass.getDeclaredFields.filter(!_.getName.equals("transactionId")).map {
      field =>
        field.setAccessible(true)
        field.getName -> (field.get(formData) match {
          case Some(x) => x.toString
          case x => x.toString
        })
    }.toMap[String, String]
  }

  private[paye] def audit(auditEvent: AuditEvent)(implicit request: Request[_]): Unit = {
    implicit val hc = HeaderCarrier(request)
    auditConnector.audit(auditEvent)
  }
}
