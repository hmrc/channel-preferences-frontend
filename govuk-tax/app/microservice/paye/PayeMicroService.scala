package microservice.paye

import microservice.{ TaxRegimeMicroService, MicroServiceConfig }

import microservice.paye.domain.{ Benefit, PayeRoot }
import play.Logger
import org.joda.time.LocalDate

class PayeMicroService extends TaxRegimeMicroService[PayeRoot] {

  import org.json4s._
  import org.json4s.jackson.JsonMethods._
  import play.api.libs.json.Json

  implicit val formats = DefaultFormats

  override val serviceUrl = MicroServiceConfig.payeServiceUrl

  def root(uri: String) = httpGet[PayeRoot](uri).getOrElse(throw new IllegalStateException(s"Expected Paye root not found at URI '$uri'"))

  override def linkedResource[T](uri: String)(implicit m: Manifest[T]) = {
    Logger.debug(s"Loading linked paye resource, uri: $uri")
    httpGet[T](uri)
  }

  def removeCarBenefit(nino: String, version: Int, benefit: Benefit, dateCarWithdrawn: LocalDate) = {

    benefit.copy(grossAmount = 0, cars = List(benefit.cars(0).copy(dateCarWithdrawn = Some(dateCarWithdrawn))))

    httpPost[Map[String, String]](benefit.actions("updateCar"), Json.parse(compact(render(Extraction.decompose(benefit)))), Map("Version" -> version.toString))

  }

}
