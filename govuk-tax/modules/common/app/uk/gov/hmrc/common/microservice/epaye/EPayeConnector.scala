package uk.gov.hmrc.common.microservice.epaye

import uk.gov.hmrc.microservice.{ MicroService, MicroServiceConfig }
import uk.gov.hmrc.common.microservice.epaye.domain.EPayeDomain.{EPayeJsonRoot, EPayeAccountSummary, EPayeRoot}
import play.api.Logger
import uk.gov.hmrc.domain.EmpRef

class EPayeConnector extends MicroService {

  override val serviceUrl = MicroServiceConfig.epayeServiceUrl

  def root(uri: String, empRef : EmpRef): EPayeRoot = {
      EPayeRoot(httpGet[EPayeJsonRoot](uri).getOrElse(throw new IllegalStateException(s"Expected EPaye root not found for resource $uri")), empRef)
  }

  def accountSummary(uri: String): Option[EPayeAccountSummary] = {
    httpGet[EPayeAccountSummary](uri) match {
      case Some(EPayeAccountSummary(None, None)) => Logger.warn(s"Empty account summary returned from EPAYE service for uri: $uri"); None
      case other => other
    }
  }
}
