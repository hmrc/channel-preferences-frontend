package uk.gov.hmrc.microservice.paye

import play.Logger
import org.joda.time.LocalDate
import views.formatting.Dates
import uk.gov.hmrc.microservice.paye.domain.{ PayeRoot, TransactionId, RemoveBenefit, Benefit }
import uk.gov.hmrc.microservice.{ TaxRegimeMicroService, MicroServiceConfig }
import controllers.common.domain.Transform._
import play.api.libs.json.Json

class PayeMicroService extends TaxRegimeMicroService[PayeRoot] {

  override val serviceUrl = MicroServiceConfig.payeServiceUrl

  def root(uri: String) = httpGet[PayeRoot](uri).getOrElse(throw new IllegalStateException(s"Expected Paye root not found at URI '$uri'"))

  override def linkedResource[T](uri: String)(implicit m: Manifest[T]) = {
    Logger.debug(s"Loading linked paye resource, uri: $uri")
    httpGet[T](uri)
  }

  def removeBenefit(uri: String, nino: String,
    version: Int,
    benefit: Benefit,
    dateCarWithdrawn: LocalDate,
    revisedGrossAmount: BigDecimal): Option[TransactionId] = {
    httpPost[TransactionId](
      uri,
      body = Json.parse(
        toRequestBody(
          RemoveBenefit(
            version = version,
            benefit = benefit,
            revisedAmount = revisedGrossAmount,
            withdrawDate = dateCarWithdrawn)
        )
      )
    )
  }

  def calculateWithdrawBenefit(benefit: Benefit, withdrawDate: LocalDate) =
    httpGet[CalculationResult](benefit.calculations("withdraw").replace("{withdrawDate}", Dates.shortDate(withdrawDate))).get
}

case class CalculationResult(result: Map[String, BigDecimal])
