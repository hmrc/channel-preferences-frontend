package uk.gov.hmrc.common.microservice.paye

import play.Logger
import org.joda.time.LocalDate
import views.formatting.Dates
import uk.gov.hmrc.common.microservice.paye.domain._
import uk.gov.hmrc.microservice.{TaxRegimeConnector, MicroServiceConfig}
import controllers.common.domain.Transform._
import play.api.libs.json.Json
import uk.gov.hmrc.common.microservice.paye.domain.PayeRoot
import uk.gov.hmrc.common.microservice.paye.domain.Benefit
import uk.gov.hmrc.common.microservice.paye.domain.RemoveBenefit
import scala.util.control.Exception._

class PayeConnector extends TaxRegimeConnector[PayeRoot] {

  val exWrapper = allCatch.withApply(e => throw new RuntimeException(e))

  override val serviceUrl = MicroServiceConfig.payeServiceUrl

  def root(uri: String) = httpGet[PayeRoot](uri).getOrElse(throw new IllegalStateException(s"Expected Paye root not found at URI '$uri'"))

  override def linkedResource[T](uri: String)(implicit m: Manifest[T]) = {
    Logger.debug(s"Loading linked paye resource, uri: $uri")
    httpGet[T](uri)
  }

  def addBenefits(uri: String,
    version: Int,
    employmentSequenceNumber:Int,
    benefits: Seq[Benefit]) : Option[AddBenefitResponse] = exWrapper {

    httpPost[AddBenefitResponse](
      uri,
      body = Json.parse(
        toRequestBody(
          AddBenefit(
            version = version,
            employmentSequence = employmentSequenceNumber,
            benefits = benefits)
        )
      )
    )
  }

  def removeBenefits(uri: String,
    version: Int,
    benefits: Seq[RevisedBenefit],
    dateCarWithdrawn: LocalDate) = exWrapper {
    httpPost[RemoveBenefitResponse](
      uri,
      body = Json.parse(
        toRequestBody(
          RemoveBenefit(
            version = version,
            benefits = benefits,
            withdrawDate = dateCarWithdrawn)
        )
      )
    )
  }

  def calculateBenefitValue(uri: String, carAndFuel: CarAndFuel): Option[NewBenefitCalculationResponse] = exWrapper {
    httpPost[NewBenefitCalculationResponse](
      uri,
      body = Json.parse(
        toRequestBody(
          carAndFuel
        )
      )
    )
  }

  def calculationWithdrawKey():String = "withdraw"
  def calculateWithdrawBenefit(benefit: Benefit, withdrawDate: LocalDate) = exWrapper {
    httpGet[RemoveBenefitCalculationResponse](benefit.calculations(calculationWithdrawKey).replace("{withdrawDate}", Dates.shortDate(withdrawDate))).get
  }
}
