package uk.gov.hmrc.common.microservice.vat

import uk.gov.hmrc.microservice.{MicroService, MicroServiceConfig}
import uk.gov.hmrc.common.microservice.vat.domain.{VatAccountSummary, VatJsonRoot}

class VatConnector extends MicroService {

  val serviceUrl = MicroServiceConfig.vatServiceUrl

  def root(uri: String) = httpGet[VatJsonRoot](uri).getOrElse(VatJsonRoot(Map.empty))

  def accountSummary(uri: String) = httpGet[VatAccountSummary](uri)
}
