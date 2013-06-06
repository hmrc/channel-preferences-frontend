package microservice.paye

import microservice.{ TaxRegimeMicroService, MicroServiceConfig }
import microservice.paye.domain.{ PayeRoot, TaxCode, Employment, Benefit }

class PayeMicroService extends TaxRegimeMicroService[PayeRoot] {

  override val serviceUrl = MicroServiceConfig.payeServiceUrl

  def root(uri: String) = {
    get[PayeRoot](uri)
  }

  def taxCodes(uri: String) = get[List[TaxCode]](uri)

  def employments(uri: String) = get[List[Employment]](uri)

  def benefits(uri: String) = get[List[Benefit]](uri)
}
