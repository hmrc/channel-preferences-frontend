package controllers.paye

import uk.gov.hmrc.common.BaseSpec
import play.api.test.{FakeApplication, WithApplication, FakeRequest}
import models.paye.AddCar

class PayeQuestionnaireUtilsSpec extends BaseSpec {

  "Mapping the questionnaire form" should {

    "succeed to extract all questionnaire fields when a user answers all questions" in new WithApplication(FakeApplication())  {

      implicit val request = FakeRequest().withFormUrlEncodedBody(("transactionId", "someTxId"), ("journeyType", "AddCar"), ("oldTaxCode", "someOldCode"),("newTaxCode", "someNewCode"),
                                                                  ("q1", "4"), ("q2", "1"), ("q3", "2"), ("q4", "4"),
                                                                  ("q5", "3"),("q6", "1"),("q7", "Some Comments"))

      val actualQuestionnaireForm = PayeQuestionnaireUtils.payeQuestionnaireForm.bindFromRequest()

      actualQuestionnaireForm.hasErrors shouldBe false
      val actualQuestionnaireData = actualQuestionnaireForm.get

      actualQuestionnaireData should have (
        'transactionId("someTxId"),
        'journeyType(Some("AddCar")),
        'newTaxCode(Some("someNewCode")),
        'oldTaxCode(Some("someOldCode")),
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
      implicit val request = FakeRequest().withFormUrlEncodedBody(("transactionId", "someTxId"), ("journeyType", "AddCar"), ("q1", "4"), ("q3", "2"), ("q4", "4"),
        ("q5", "3"),("q7", "Some Comments"))

      val actualQuestionnaireForm = PayeQuestionnaireUtils.payeQuestionnaireForm.bindFromRequest()

      actualQuestionnaireForm.hasErrors shouldBe false
      val actualQuestionnaireData = actualQuestionnaireForm.get

      actualQuestionnaireData should have (
        'transactionId("someTxId"),
        'journeyType(Some("AddCar")),
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
      implicit val request = FakeRequest().withFormUrlEncodedBody(("transactionId", "someTxId"), ("journeyType", "AddCar"))

      val actualQuestionnaireForm = PayeQuestionnaireUtils.payeQuestionnaireForm.bindFromRequest()

      actualQuestionnaireForm.hasErrors shouldBe false
      val actualQuestionnaireData = actualQuestionnaireForm.get

      actualQuestionnaireData should have (
        'transactionId("someTxId"),
        'journeyType(Some("AddCar")),
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

    "fail to extract the questionnaire fields if someone has entered invalid in the ratings" in {
      implicit val request = FakeRequest().withFormUrlEncodedBody("transactionId" -> "someTxId", "journeyType"-> "AddCar", "oldTaxCode" -> "someOldCode","newTaxCode" -> "someNewCode",
        "q1" -> "6", "q2" -> "-1", "q3" -> "0", "q4" -> "22222", "q5" -> "0", "q6" -> "50", "q7" -> "Some text.")
      val results = PayeQuestionnaireUtils.payeQuestionnaireForm.bindFromRequest()
      
      results should have('hasErrors (true))

      import PayeQuestionnaireUtils.FormFields._
      for (questionnaireType <- List(wasItEasy, secure, comfortable, easyCarUpdateDetails, onlineNextTime, overallSatisfaction)) withClue(questionnaireType + " errors ") {
        results.errors(questionnaireType) should not be empty
      }
    }
    "fail to extract the questionnaire fields if someone has entered excessively long text in the extra comments field" in {
      implicit val request = FakeRequest().withFormUrlEncodedBody("transactionId" -> "someTxId", "journeyType"-> "AddCar", "oldTaxCode" -> "someOldCode","newTaxCode" -> "someNewCode",
        "q1" -> "1", "q2" -> "1", "q3" -> "1", "q4" -> "2", "q5" -> "1", "q6" -> "5", "q7" -> "A" * 10001)
      val results = PayeQuestionnaireUtils.payeQuestionnaireForm.bindFromRequest()

      results should have('hasErrors (true))

      import PayeQuestionnaireUtils.FormFields._
      results.errors(commentForImprovements) should not be empty
    }
  }

  "toJourneyType" should {

    "return a valid PayeJourney object if the given string represents one" in {
      PayeQuestionnaireUtils.toJourneyType("AddCar") shouldBe AddCar
    }

    "throw an exception if the given string does not represent a valid PayeJourney" in {
      evaluating(PayeQuestionnaireUtils.toJourneyType("wongType")) should produce [IllegalJourneyTypeException]
    }
  }
}
