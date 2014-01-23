package controllers.paye

import uk.gov.hmrc.common.microservice.audit.{AuditEvent, AuditConnector}
import uk.gov.hmrc.common.microservice.keystore.KeyStoreConnector
import play.api.mvc.Request
import controllers.common.actions.HeaderCarrier
import scala.concurrent.ExecutionContext.Implicits.global

class QuestionnaireAuditor(val auditConnector: AuditConnector, val keystore: KeyStoreConnector) {

  def auditOnce(auditEvent: AuditEvent, transactionId: String)(implicit request: Request[_]) : Unit = {
    implicit val hc = HeaderCarrier(request)
    val keystoreResponse = keystore.getEntry[String]("QuestionnaireFormDataSubmission", KeystoreUtils.source, transactionId)
    keystoreResponse.map{_.getOrElse{
      keystore.addKeyStoreEntry[String]("QuestionnaireFormDataSubmission", KeystoreUtils.source, transactionId, "HasBeenSubmitted")
      audit(auditEvent)
    }}
  }

  private def audit(auditEvent: AuditEvent)(implicit hc: HeaderCarrier): Unit = {
    auditConnector.audit(auditEvent)
  }
}