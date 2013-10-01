package uk.gov.hmrc.common.microservice.ct

import uk.gov.hmrc.microservice.{ MicroService, MicroServiceConfig }
import uk.gov.hmrc.common.microservice.ct.domain.CtDomain.{CtJsonRoot, CtAccountSummary}

class CtConnector extends MicroService {

  override val serviceUrl = MicroServiceConfig.ctServiceUrl

  def root(uri: String): CtJsonRoot = httpGet[CtJsonRoot](uri).getOrElse(throw new IllegalStateException(s"Expected Ct root not found for resource $uri"))

  def accountSummary(uri: String): Option[CtAccountSummary] = {
    httpGet[CtAccountSummary](uri)
  }

}
