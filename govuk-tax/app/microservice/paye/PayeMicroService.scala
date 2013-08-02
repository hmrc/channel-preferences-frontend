package microservice.paye

import microservice.{ TaxRegimeMicroService, MicroServiceConfig }

import microservice.paye.domain.{ TransactionId, RemoveCarBenefit, Benefit, PayeRoot }
import play.Logger
import org.joda.time.LocalDate
import views.formatting.Dates

class PayeMicroService extends TaxRegimeMicroService[PayeRoot] {

  import controllers.domain.Transform._
  import play.api.libs.json.Json

  override val serviceUrl = MicroServiceConfig.payeServiceUrl

  def root(uri: String) = httpGet[PayeRoot](uri).getOrElse(throw new IllegalStateException(s"Expected Paye root not found at URI '$uri'"))

  override def linkedResource[T](uri: String)(implicit m: Manifest[T]) = {
    Logger.debug(s"Loading linked paye resource, uri: $uri")
    httpGet[T](uri)
  }

  def removeCarBenefit(nino: String, version: Int, benefit: Benefit, dateCarWithdrawn: LocalDate, revisedGrossAmount: BigDecimal): Option[TransactionId] = {
    httpPost[TransactionId](benefit.actions("removeCar"), Json.parse(toRequestBody(RemoveCarBenefit(version, benefit, revisedGrossAmount, dateCarWithdrawn))))
  }

  def calculateWithdrawBenefit(benefit: Benefit, withdrawDate: LocalDate): CalculationResult = {
    httpGet[CalculationResult](benefit.calculations("withdraw").replace("{withdrawDate}", Dates.shortDate(withdrawDate))).get
  }

}
case class CalculationResult(result: Map[String, BigDecimal])
