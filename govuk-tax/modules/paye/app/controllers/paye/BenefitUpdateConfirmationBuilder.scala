package controllers.paye

import models.paye.BenefitUpdatedConfirmationData
import uk.gov.hmrc.common.microservice.paye.domain.AddBenefitResponse
import org.joda.time.LocalDate

object BenefitUpdateConfirmationBuilder {

  def buildBenefitUpdatedConfirmationData(currentTaxYearCode: String, addBenefitsResponse: AddBenefitResponse): BenefitUpdatedConfirmationData = {
     BenefitUpdatedConfirmationData(addBenefitsResponse.transaction.oid, currentTaxYearCode, addBenefitsResponse.newTaxCode)
  }
}
