package controllers.paye

import uk.gov.hmrc.common.BaseSpec
import play.api.test.Helpers._
import play.api.test.{FakeRequest, FakeApplication, WithApplication}

class PayeQuestionnaireControllerSpec extends BaseSpec {

  private lazy val controller = new PayeQuestionnaireController

  "buildAuditEvent " should {

    "create an audit event given a paye questionnaire form data containing every field" in new WithApplication(FakeApplication()) {
      val formData = PayeQuestionnaireFormData(
        transactionId = "someTxId",
        wasItEasy = Some(1),
        secure = Some(2),
        comfortable = Some(3),
        easyCarUpdateDetails = Some(4),
        onlineNextTime = Some(3),
        overallSatisfaction = Some(2),
        commentForImprovements = Some("comment")
      )

      val actualAuditEvent = controller.buildAuditEvent(formData)

      val detail = Map[String, String]("wasItEasy" -> "1", "secure" -> "2", "comfortable" -> "3",
          "easyCarUpdateDetails" -> "4", "onlineNextTime" -> "3", "overallSatisfaction" -> "2", "commentForImprovements" -> "comment")
      actualAuditEvent should have (
        'auditSource("frontend"),
        'auditType("questionnaire"),
        'tags(Map("questionnaire-transactionId" -> "someTxId")),
        'detail(detail)
      )
    }

    "create an audit event given a paye questionnaire form data containing transactionId only" in new WithApplication(FakeApplication()) {
      val formData = PayeQuestionnaireFormData(
        transactionId = "someTxId"
      )

      val actualAuditEvent = controller.buildAuditEvent(formData)

      val detail = Map[String, String]("wasItEasy" -> "None", "secure" -> "None", "comfortable" -> "None",
        "easyCarUpdateDetails" -> "None", "onlineNextTime" -> "None", "overallSatisfaction" -> "None", "commentForImprovements" -> "None")
      actualAuditEvent should have (
        'auditSource("frontend"),
        'auditType("questionnaire"),
        'tags(Map("questionnaire-transactionId" -> "someTxId")),
        'detail(detail)
      )
    }
  }

  "submitQuestionnaireAction" should {

    "return 200 if no errors are encountered" in  new WithApplication(FakeApplication()) {
      implicit val request = FakeRequest().withFormUrlEncodedBody(("transactionId", "someTxId"), ("q1", "4"), ("q3", "2"), ("q4", "4"),
        ("q5", "3"),("q7", "Some Comments"))

      val result = controller.submitQuestionnaireAction

      status(result) shouldBe 200
    }

    "return 400 if the form has no transactionId field" in  new WithApplication(FakeApplication()) {
      implicit val request = FakeRequest().withFormUrlEncodedBody(("q1", "4"), ("q3", "2"), ("q4", "4"),
        ("q5", "3"),("q7", "Some Comments"))

      val result = controller.submitQuestionnaireAction

      status(result) shouldBe 400
    }
  }

}
