package uk.gov.hmrc.microservice.paye

import play.Logger
import org.joda.time.LocalDate
import views.formatting.Dates
import uk.gov.hmrc.microservice.paye.domain._
import uk.gov.hmrc.microservice.{ TaxRegimeMicroService, MicroServiceConfig }
import controllers.common.domain.Transform._
import play.api.libs.json.Json
import uk.gov.hmrc.microservice.paye.domain.PayeRoot
import uk.gov.hmrc.microservice.paye.CalculationResult
import uk.gov.hmrc.microservice.paye.domain.Benefit
import uk.gov.hmrc.microservice.paye.domain.TransactionId
import uk.gov.hmrc.microservice.paye.domain.RemoveBenefit

class PayeMicroService extends TaxRegimeMicroService[PayeRoot] {

  override val serviceUrl = MicroServiceConfig.payeServiceUrl

  def root(uri: String) = httpGet[PayeRoot](uri).getOrElse(throw new IllegalStateException(s"Expected Paye root not found at URI '$uri'"))

  override def linkedResource[T](uri: String)(implicit m: Manifest[T]) = {
    Logger.debug(s"Loading linked paye resource, uri: $uri")
    httpGet[T](uri)
  }

  def removeBenefits(uri: String, nino: String,
    version: Int,
    benefits: Seq[RevisedBenefit],
    dateCarWithdrawn: LocalDate): Option[TransactionId] = {
    httpPost[TransactionId](
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

  def calculateWithdrawBenefit(benefit: Benefit, withdrawDate: LocalDate) =
    httpGet[CalculationResult](benefit.calculations("withdraw").replace("{withdrawDate}", Dates.shortDate(withdrawDate))).get
}

case class CalculationResult(result: Map[String, BigDecimal])
