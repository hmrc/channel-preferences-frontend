package controllers.paye

import models.paye.BenefitUpdatedConfirmationData
import uk.gov.hmrc.common.microservice.paye.domain.WriteBenefitResponse

object BenefitUpdateConfirmationBuilder {

  def buildBenefitUpdatedConfirmationData(currentTaxYearCode: String, writeBenefitResponse: WriteBenefitResponse): BenefitUpdatedConfirmationData = {
     BenefitUpdatedConfirmationData(writeBenefitResponse.transaction.oid, currentTaxYearCode, writeBenefitResponse.taxCode)
  }
}
