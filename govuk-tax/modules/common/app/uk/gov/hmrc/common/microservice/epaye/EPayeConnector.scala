package uk.gov.hmrc.common.microservice.epaye

import uk.gov.hmrc.microservice.{ MicroService, MicroServiceConfig }
import uk.gov.hmrc.common.microservice.epaye.domain.EpayeDomain.{EpayeJsonRoot, EpayeAccountSummary}
import play.api.Logger

class EpayeConnector extends MicroService {

  override val serviceUrl = MicroServiceConfig.epayeServiceUrl

  def root(uri: String): EpayeJsonRoot = {
      httpGet[EpayeJsonRoot](uri).getOrElse(throw new IllegalStateException(s"Expected Epaye root not found for resource $uri"))
  }

  def accountSummary(uri: String): Option[EpayeAccountSummary] = {
    httpGet[EpayeAccountSummary](uri) match {
      case Some(EpayeAccountSummary(None, None)) => Logger.warn(s"Empty account summary returned from EPAYE service for uri: $uri"); None
      case other => other
    }
  }
}
