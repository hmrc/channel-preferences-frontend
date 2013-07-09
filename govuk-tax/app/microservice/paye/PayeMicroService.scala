package microservice.paye

import microservice.{ TaxRegimeMicroService, MicroServiceConfig }

import microservice.paye.domain.{ Benefit, PayeRoot }
import play.Logger
import org.joda.time.LocalDate

class PayeMicroService extends TaxRegimeMicroService[PayeRoot] {

  import controllers.domain.Transform._
  import play.api.libs.json.Json

  override val serviceUrl = MicroServiceConfig.payeServiceUrl

  def root(uri: String) = httpGet[PayeRoot](uri).getOrElse(throw new IllegalStateException(s"Expected Paye root not found at URI '$uri'"))

  override def linkedResource[T](uri: String)(implicit m: Manifest[T]) = {
    Logger.debug(s"Loading linked paye resource, uri: $uri")
    httpGet[T](uri)
  }

  def removeCarBenefit(nino: String, version: Int, benefit: Benefit, dateCarWithdrawn: LocalDate) = {

    val deletedBenefit = benefit.copy(grossAmount = 0, cars = List(benefit.cars(0).copy(dateCarWithdrawn = Some(dateCarWithdrawn))))

    httpPost[Map[String, String]](deletedBenefit.actions("updateCar"), Json.parse(toRequestBody(deletedBenefit)), Map("Version" -> version.toString))

  }

}
