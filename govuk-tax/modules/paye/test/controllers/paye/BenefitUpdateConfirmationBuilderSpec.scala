package controllers.paye

import uk.gov.hmrc.common.BaseSpec
import models.paye.BenefitUpdatedConfirmationData
import org.joda.time.LocalDate
import controllers.paye.BenefitUpdateConfirmationBuilder._
import uk.gov.hmrc.common.microservice.paye.domain.{TransactionId, AddBenefitResponse}

class BenefitUpdateConfirmationBuilderSpec extends BaseSpec {

  "Build a benefit update confirmation data object" should {

    "return the object populated with all the fields" in {

      val taxYearStart = new LocalDate(2013, 1, 25)
      val taxYearEnd = new LocalDate(2013, 1, 25)
      val expectedBenefitUpdateConfirmationData = BenefitUpdatedConfirmationData(
        transactionId = "someId",
        oldTaxCode = "oldTaxCode",
        newTaxCode = Some("newTaxCode")
      )

      val addBenefitsResponse = AddBenefitResponse(TransactionId("someId"), Some("newTaxCode"), Some(125))
      val actualBenefitUpdateConfirmationData = buildBenefitUpdatedConfirmationData("oldTaxCode", addBenefitsResponse)

      actualBenefitUpdateConfirmationData shouldBe expectedBenefitUpdateConfirmationData
    }
  }


}
