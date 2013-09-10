package uk.gov.hmrc.common.microservice.vat

import uk.gov.hmrc.microservice.{ MicroService, MicroServiceConfig }
import uk.gov.hmrc.common.microservice.vat.domain.VatDomain.{ VatRoot, VatAccountSummary }

class VatMicroService extends MicroService {

  override val serviceUrl = MicroServiceConfig.vatServiceUrl

  def root(uri: String): VatRoot = httpGet[VatRoot](uri).getOrElse(throw new IllegalStateException(s"Expected VAT root not found for resource $uri"))

  def accountSummary(uri: String): Option[VatAccountSummary] = {
    httpGet[VatAccountSummary](uri)
  }

}
