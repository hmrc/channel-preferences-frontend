package uk.gov.hmrc.common.microservice.epaye

import uk.gov.hmrc.microservice.{ MicroService, MicroServiceConfig }
import uk.gov.hmrc.common.microservice.epaye.domain.EPayeDomain.{EPayeAccountSummary, EPayeRoot}
import play.api.Logger

class EPayeConnector extends MicroService {

  override val serviceUrl = MicroServiceConfig.epayeServiceUrl

  def root(uri: String): EPayeRoot = {
      httpGet[EPayeRoot](uri).getOrElse(throw new IllegalStateException(s"Expected EPaye root not found for resource $uri"))
  }

  def accountSummary(uri: String): Option[EPayeAccountSummary] = {
    httpGet[EPayeAccountSummary](uri) match {
      case Some(EPayeAccountSummary(None, None)) => Logger.warn(s"Empty account summary returned from EPAYE service for uri: $uri"); None
      case other => other
    }
  }
}
