package microservice.paye

import microservice.{ TaxRegimeMicroService, MicroServiceConfig }

import microservice.paye.domain.{ Benefit, PayeRoot }
import play.Logger

class PayeMicroService extends TaxRegimeMicroService[PayeRoot] {

  override val serviceUrl = MicroServiceConfig.payeServiceUrl

  //todo ? bring back SA stuff here because SA is also personal stuff

  def root(uri: String) = httpGet[PayeRoot](uri).getOrElse(throw new IllegalStateException(s"Expected Paye root not found at URI '$uri'"))

  override def linkedResource[T](uri: String)(implicit m: Manifest[T]) = {
    Logger.debug(s"Loading linked paye resource, uri: $uri")
    httpGet[T](uri)
  }

  def removeCarBenefit(nino: String, employment: Int, benefit: Benefit) = {

    benefit.copy(grossAmount = 0)

    httpPost[Map[String, String]](s"/paye/$nino/benefits/2013/$employment/update/car", null)

  }

  //  @Path("/{nino}/benefits/{year}/{employment}/update/car")
}
