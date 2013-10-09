package uk.gov.hmrc.common.microservice.vat

import uk.gov.hmrc.microservice.{ MicroService, MicroServiceConfig }
import uk.gov.hmrc.common.microservice.vat.domain.VatDomain.{VatJsonRoot, VatRoot, VatAccountSummary}

class VatConnector extends MicroService {

  override val serviceUrl = MicroServiceConfig.vatServiceUrl

  def root(uri: String): VatJsonRoot = httpGet[VatJsonRoot](uri).getOrElse(VatJsonRoot(Map.empty))

  def accountSummary(uri: String): Option[VatAccountSummary] = {
    httpGet[VatAccountSummary](uri)
  }

}
