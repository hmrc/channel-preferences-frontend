package controllers.paye

import uk.gov.hmrc.common.microservice.audit.{AuditEvent, AuditConnector}
import uk.gov.hmrc.common.microservice.keystore.KeyStoreConnector
import controllers.common.actions.HeaderCarrier
import uk.gov.hmrc.common.MdcLoggingExecutionContext._
import com.typesafe.scalalogging.slf4j.Logging

class QuestionnaireAuditor(val auditConnector: AuditConnector, val keystore: KeyStoreConnector) {

  val keystoreFormName = "QuestionnaireFormDataSubmission"

  def auditOnce(auditEvent: AuditEvent, transactionId: String)(implicit hc: HeaderCarrier): Unit = {
    val keystoreResponse = keystore.getEntry[QuestionanaireData](keystoreFormName, KeystoreUtils.source, transactionId)
    keystoreResponse.map {
      _.getOrElse {
        keystore.addKeyStoreEntry[QuestionanaireData](keystoreFormName, KeystoreUtils.source, transactionId, QuestionanaireData("HasBeenSubmitted"))
        audit(auditEvent)
      }
    }
  }

  private def audit(auditEvent: AuditEvent)(implicit hc: HeaderCarrier): Unit = {
    auditConnector.audit(auditEvent)
  }
}

private[paye] case class QuestionanaireData(value: String)