package microservice.paye

import microservice.{ TaxRegimeMicroService, MicroServiceConfig }

import microservice.paye.domain.PayeRoot
import play.Logger
import play.api.libs.json.Json
import org.joda.time.LocalDate

class PayeMicroService extends TaxRegimeMicroService[PayeRoot] {

  override val serviceUrl = MicroServiceConfig.payeServiceUrl

  //todo ? bring back SA stuff here because SA is also personal stuff

  def root(uri: String) = httpGet[PayeRoot](uri).getOrElse(throw new IllegalStateException(s"Expected Paye root not found at URI '$uri'"))

  override def linkedResource[T](uri: String)(implicit m: Manifest[T]) = {
    Logger.debug(s"Loading linked paye resource, uri: $uri")
    httpGet[T](uri)
  }

  def removeCarBenefit(nino: String, benefitId: Int, carId: Int, dateRemoved: LocalDate) =
    httpPost[Map[String, String]](s"/paye/$nino/remove_benefit", Json.obj("benefitId" -> benefitId, "carId" -> carId, "dateRemoved" -> dateRemoved))

}
