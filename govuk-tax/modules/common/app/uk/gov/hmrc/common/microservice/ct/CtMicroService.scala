package ct

import ct.domain.CtDomain.{CtRoot, CtAccountSummary}
import uk.gov.hmrc.microservice.{ MicroService, MicroServiceConfig }

class CtMicroService extends MicroService {

  override val serviceUrl = MicroServiceConfig.ctServiceUrl

  def root(uri: String): CtRoot = httpGet[CtRoot](uri).getOrElse(throw new IllegalStateException(s"Expected Ct root not found for resource $uri"))

  def accountSummary(uri: String): Option[CtAccountSummary] = {
    httpGet[CtAccountSummary](uri)
  }

}
