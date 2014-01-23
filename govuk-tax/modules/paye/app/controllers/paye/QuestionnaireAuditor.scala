package controllers.paye

import uk.gov.hmrc.common.microservice.audit.{AuditEvent, AuditConnector}
import uk.gov.hmrc.common.microservice.keystore.KeyStoreConnector
import controllers.common.actions.HeaderCarrier
import uk.gov.hmrc.common.MdcLoggingExecutionContext._

class QuestionnaireAuditor(val auditConnector: AuditConnector, val keystore: KeyStoreConnector) {

  def auditOnce(auditEvent: AuditEvent, transactionId: String)(implicit hc: HeaderCarrier): Unit = {
    val keystoreResponse = keystore.getEntry[String]("QuestionnaireFormDataSubmission", KeystoreUtils.source, transactionId)
    keystoreResponse.map {
      _.getOrElse {
        keystore.addKeyStoreEntry[String]("QuestionnaireFormDataSubmission", KeystoreUtils.source, transactionId, "HasBeenSubmitted")
        audit(auditEvent)
      }
    }
  }

  private def audit(auditEvent: AuditEvent)(implicit hc: HeaderCarrier): Unit = {
    auditConnector.audit(auditEvent)
  }
}