package microservice.paye

import microservice.{ TaxRegimeMicroService, MicroServiceConfig }
import microservice.paye.domain.{ SaRoot, PayeRoot }

class PayeMicroService extends TaxRegimeMicroService[PayeRoot] {
  override val serviceUrl = MicroServiceConfig.personalServiceUrl
}

class PersonalSAMicroService extends TaxRegimeMicroService[SaRoot] {
  override val serviceUrl = MicroServiceConfig.personalServiceUrl
}
