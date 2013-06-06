package microservice.paye

import microservice.{ TaxRegimeMicroService, MicroServiceConfig }
import microservice.paye.domain.{ PayeRoot, TaxCode }

class PayeMicroService extends TaxRegimeMicroService[PayeRoot] {

  override val serviceUrl = MicroServiceConfig.payeServiceUrl

  // this throws because if it's being called, we expect a PayeRoot to be present
  def root(uri: String) = get[PayeRoot](uri).getOrElse(throw new IllegalStateException(s"Expected Paye root not found at URI '$uri'"))

  def taxCodes(uri: String) = get[Seq[TaxCode]](uri)
}
