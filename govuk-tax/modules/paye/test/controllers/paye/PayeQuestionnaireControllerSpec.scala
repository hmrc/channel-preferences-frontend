package controllers.paye

import uk.gov.hmrc.common.BaseSpec
import play.api.test.{FakeApplication, WithApplication, FakeRequest}

class PayeQuestionnaireControllerSpec extends BaseSpec {

  "Mapping the questionnaire form" should {

    //val controller = new PayeQuestionnaireController()

    "extract all questionnaire fields when a user answers all questions" in new WithApplication(FakeApplication())  {

      implicit val request = FakeRequest().withFormUrlEncodedBody(("transactionId", "someTxId"), ("q1", "4"), ("q2", "1"), ("q3", "2"), ("q4", "4"),
                                                                  ("q5", "3"),("q6", "1"),("q7", "Some Comments"))

      val actualQuestionnaireForm = payeQuestionnaireUtils.payeQuestionnaireForm.bindFromRequest()

      actualQuestionnaireForm.hasErrors shouldBe false
      val actualQuestionnaireData = actualQuestionnaireForm.get

      actualQuestionnaireData.wasItEasy shouldBe Some(4)

//      actualQuestionnaireData should have {
//        'transactionId("someTxId")
//        'wasItEasy("Some(4)")
//        'q2("1")
//        'q3("2")
//        'q4("4")
//        'q5("3")
//        'q6("1")
//        'q7("Some Comments")
//      }
    }

    "extract all questionnaire fields when a user answers some questions" in {

    }

    "extract all questionnaire fields when a user does not answer any questions" in {

    }

    "extact a transaction id" in {

    }
  }
}
