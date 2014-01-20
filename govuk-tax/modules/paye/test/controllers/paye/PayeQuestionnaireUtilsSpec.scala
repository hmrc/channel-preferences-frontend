package controllers.paye

import uk.gov.hmrc.common.BaseSpec
import play.api.test.{FakeApplication, WithApplication, FakeRequest}

class PayeQuestionnaireUtilsSpec extends BaseSpec {

  "Mapping the questionnaire form" should {

    "succeed to extract all questionnaire fields when a user answers all questions" in new WithApplication(FakeApplication())  {

      implicit val request = FakeRequest().withFormUrlEncodedBody(("transactionId", "someTxId"), ("q1", "4"), ("q2", "1"), ("q3", "2"), ("q4", "4"),
                                                                  ("q5", "3"),("q6", "1"),("q7", "Some Comments"))

      val actualQuestionnaireForm = PayeQuestionnaireUtils.payeQuestionnaireForm.bindFromRequest()

      actualQuestionnaireForm.hasErrors shouldBe false
      val actualQuestionnaireData = actualQuestionnaireForm.get

      actualQuestionnaireData should have (
        'transactionId("someTxId"),
        'wasItEasy(Some(4)),
        'secure(Some(1)),
        'comfortable(Some(2)),
        'easyCarUpdateDetails(Some(4)),
        'onlineNextTime(Some(3)),
        'overallSatisfaction(Some(1)),
        'commentForImprovements(Some("Some Comments"))
      )
    }

    "succeed to extract all questionnaire fields when a user answers some questions" in {
      implicit val request = FakeRequest().withFormUrlEncodedBody(("transactionId", "someTxId"), ("q1", "4"), ("q3", "2"), ("q4", "4"),
        ("q5", "3"),("q7", "Some Comments"))

      val actualQuestionnaireForm = PayeQuestionnaireUtils.payeQuestionnaireForm.bindFromRequest()

      actualQuestionnaireForm.hasErrors shouldBe false
      val actualQuestionnaireData = actualQuestionnaireForm.get

      actualQuestionnaireData should have (
        'transactionId("someTxId"),
        'wasItEasy(Some(4)),
        'secure(None),
        'comfortable(Some(2)),
        'easyCarUpdateDetails(Some(4)),
        'onlineNextTime(Some(3)),
        'overallSatisfaction(None),
        'commentForImprovements(Some("Some Comments"))
      )
    }

    "succeed to extract all questionnaire fields when a user does not answer any questions" in {
      implicit val request = FakeRequest().withFormUrlEncodedBody(("transactionId", "someTxId"))

      val actualQuestionnaireForm = PayeQuestionnaireUtils.payeQuestionnaireForm.bindFromRequest()

      actualQuestionnaireForm.hasErrors shouldBe false
      val actualQuestionnaireData = actualQuestionnaireForm.get

      actualQuestionnaireData should have (
        'transactionId("someTxId"),
        'wasItEasy(None),
        'secure(None),
        'comfortable(None),
        'easyCarUpdateDetails(None),
        'onlineNextTime(None),
        'overallSatisfaction(None),
        'commentForImprovements(None)
      )
    }

    "fail to extract the questionnaire fields if the transactionId is missing" in {
      implicit val request = FakeRequest().withFormUrlEncodedBody(("q1", "4"), ("q3", "2"), ("q4", "4"),
        ("q5", "3"),("q7", "Some Comments"))

      val actualQuestionnaireForm = PayeQuestionnaireUtils.payeQuestionnaireForm.bindFromRequest()

      actualQuestionnaireForm.hasErrors shouldBe true
    }

    "fail to extract the questionnaire fields if the transactionId is empty" in {
      implicit val request = FakeRequest().withFormUrlEncodedBody(("transactionId", ""), ("q1", "4"), ("q3", "2"), ("q4", "4"),
        ("q5", "3"),("q7", "Some Comments"))

      val actualQuestionnaireForm = PayeQuestionnaireUtils.payeQuestionnaireForm.bindFromRequest()

      actualQuestionnaireForm.hasErrors shouldBe true
    }

    "fail to extract the questionnaire fields if the transactionId contains blank spaces only" in {
      implicit val request = FakeRequest().withFormUrlEncodedBody(("transactionId", "   "), ("q1", "4"), ("q3", "2"), ("q4", "4"),
        ("q5", "3"),("q7", "Some Comments"))

      val actualQuestionnaireForm = PayeQuestionnaireUtils.payeQuestionnaireForm.bindFromRequest()

      actualQuestionnaireForm.hasErrors shouldBe true
    }
  }
}
