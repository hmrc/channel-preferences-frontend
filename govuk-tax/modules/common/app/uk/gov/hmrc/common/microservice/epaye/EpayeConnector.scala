package uk.gov.hmrc.common.microservice.epaye

import uk.gov.hmrc.microservice.{ Connector, MicroServiceConfig }
import play.api.Logger
import uk.gov.hmrc.common.microservice.epaye.domain.{EpayeAccountSummary, EpayeLinks, EpayeJsonRoot}
import controllers.common.actions.HeaderCarrier
import scala.concurrent._
import ExecutionContext.Implicits.global

class EpayeConnector extends Connector {

  override val serviceUrl = MicroServiceConfig.epayeServiceUrl

  def root(uri: String)(implicit hc: HeaderCarrier): Future[EpayeJsonRoot] = {
      httpGetF[EpayeJsonRoot](uri).map(_.getOrElse(EpayeJsonRoot(EpayeLinks(None))))
  }

  def accountSummary(uri: String)(implicit headerCarrier:HeaderCarrier): Future[Option[EpayeAccountSummary]] = {
    httpGetF[EpayeAccountSummary](uri) map {
      case Some(EpayeAccountSummary(None, None)) => Logger.warn(s"Empty account summary returned from EPAYE service for uri: $uri"); None
      case success => success
    }
  }
}
