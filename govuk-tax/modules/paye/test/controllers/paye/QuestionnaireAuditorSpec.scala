package controllers.paye

import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.common.BaseSpec
import uk.gov.hmrc.common.microservice.audit.AuditConnector
import uk.gov.hmrc.common.microservice.keystore.KeyStoreConnector
import org.mockito.Mockito._
import org.mockito.Matchers.any
import org.mockito.Matchers
import scala.concurrent.Future
import scala.Predef._
import uk.gov.hmrc.common.microservice.audit.AuditEvent
import play.api.test.FakeRequest


class QuestionnaireAuditorSpec extends BaseSpec with MockitoSugar {


  val auditEvent = new AuditEvent(auditType = "some audit type")
  val transactionId = "someTxId"

  "Questionnaire Auditor" should {

    implicit val request = FakeRequest()

    //TODO: Figure out why verify is failing on CI and not locally
    "perform the audit and update the keystore if the questionnaire has not been submitted yet" in {
      val mockAuditConnector = mock[AuditConnector]
      val mockKeyStoreConnector = mock[KeyStoreConnector]
      val questionnaireAuditor = new QuestionnaireAuditor(mockAuditConnector, mockKeyStoreConnector)

      when(mockKeyStoreConnector.getEntry(Matchers.eq("QuestionnaireFormDataSubmission"), Matchers.eq(KeystoreUtils.source), Matchers.eq(transactionId), any())(any(), any())).thenReturn(Future.successful(None))

      questionnaireAuditor.auditOnce(auditEvent, transactionId)

      verify(mockAuditConnector).audit(any())(any())
      verify(mockKeyStoreConnector).addKeyStoreEntry[String](Matchers.eq("QuestionnaireFormDataSubmission"), Matchers.eq(KeystoreUtils.source), Matchers.eq(transactionId), Matchers.eq("HasBeenSubmitted"), Matchers.eq(false))(any(), any())
    }

    //TODO: Figure out why verify is failing on CI and not locally
    "not perform the audit when the questionnaire has already been submitted" in {
      val mockAuditConnector = mock[AuditConnector]
      val mockKeyStoreConnector = mock[KeyStoreConnector]
      val questionnaireAuditor = new QuestionnaireAuditor(mockAuditConnector, mockKeyStoreConnector)

      when(mockKeyStoreConnector.getEntry[String](Matchers.eq("QuestionnaireFormDataSubmission"), Matchers.eq(KeystoreUtils.source), Matchers.eq(transactionId), any())(any(), any())).thenReturn(Future.successful(Some("populated!")))

      questionnaireAuditor.auditOnce(auditEvent, transactionId)

      verifyZeroInteractions(mockAuditConnector)
      verify(mockKeyStoreConnector, never()).addKeyStoreEntry[String](any(), any(), any(), any(), any())(any(), any())
    }
  }

}
