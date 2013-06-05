package microservice.paye

import microservice.{ TaxRegimeMicroService, MicroServiceConfig }
import controllers.domain.PayeRoot
import microservice.paye.domain.TaxCode

class PayeMicroService extends TaxRegimeMicroService[PayeRoot] {

  override val serviceUrl = MicroServiceConfig.payeServiceUrl

  def root(uri: String) = get[PayeRoot](uri)

  def taxCode(uri: String) = get[TaxCode](uri)
}
