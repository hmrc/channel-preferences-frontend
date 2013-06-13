package microservice.paye

import microservice.{ TaxRegimeMicroService, MicroServiceConfig }

import microservice.paye.domain.PayeRoot
import play.Logger

class PayeMicroService extends TaxRegimeMicroService[PayeRoot] {

  override val serviceUrl = MicroServiceConfig.payeServiceUrl

  def root(uri: String) = get[PayeRoot](uri).getOrElse(throw new IllegalStateException(s"Expected Paye root not found at URI '$uri'"))

  def linkedResource[T](uri: String)(implicit m: Manifest[T]) = {
    Logger.debug(s"Loading linked paye resource, uri: $uri")
    get[T](uri)
  }

}
