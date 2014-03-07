package controllers.paye

import uk.gov.hmrc.common.BaseSpec
import models.paye.BenefitUpdatedConfirmationData
import controllers.paye.BenefitUpdateConfirmationBuilder._
import uk.gov.hmrc.common.microservice.paye.domain.{TransactionId, WriteBenefitResponse}

class BenefitUpdateConfirmationBuilderSpec extends BaseSpec {

  "Build a benefit update confirmation data object" should {

    "return the object populated with all the fields" in {

      val expectedBenefitUpdateConfirmationData = BenefitUpdatedConfirmationData(
        transactionId = "someId",
        oldTaxCode = "oldTaxCode",
        newTaxCode = Some("newTaxCode")
      )

      val writeBenefitResponse = WriteBenefitResponse(TransactionId("someId"), Some("newTaxCode"), Some(125))
      val actualBenefitUpdateConfirmationData = buildBenefitUpdatedConfirmationData("oldTaxCode", writeBenefitResponse)

      actualBenefitUpdateConfirmationData shouldBe expectedBenefitUpdateConfirmationData
    }
  }


}
