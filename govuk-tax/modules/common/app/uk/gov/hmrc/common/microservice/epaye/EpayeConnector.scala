package uk.gov.hmrc.common.microservice.epaye

import uk.gov.hmrc.microservice.{ Connector, MicroServiceConfig }
import play.api.Logger
import uk.gov.hmrc.common.microservice.epaye.domain.{EpayeAccountSummary, EpayeLinks, EpayeJsonRoot}

class EpayeConnector extends Connector {

  override val serviceUrl = MicroServiceConfig.epayeServiceUrl

  def root(uri: String): EpayeJsonRoot = {
      httpGet[EpayeJsonRoot](uri).getOrElse(EpayeJsonRoot(EpayeLinks(None)))
  }

  def accountSummary(uri: String): Option[EpayeAccountSummary] = {
    httpGet[EpayeAccountSummary](uri) match {
      case Some(EpayeAccountSummary(None, None)) => Logger.warn(s"Empty account summary returned from EPAYE service for uri: $uri"); None
      case other => other
    }
  }
}
