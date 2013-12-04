package uk.gov.hmrc.common.microservice.txqueue

import uk.gov.hmrc.microservice.{MicroServiceConfig, Connector}
import uk.gov.hmrc.common.microservice.paye.domain.PayeRoot
import uk.gov.hmrc.common.microservice.txqueue.domain.TxQueueTransaction
import controllers.common.actions.HeaderCarrier
import scala.concurrent.Future

class TxQueueConnector extends Connector {

  override val serviceUrl = MicroServiceConfig.txQueueServiceUrl

  def transaction(uri: String)(implicit hc: HeaderCarrier): Future[Option[List[TxQueueTransaction]]] =
    httpGetF[List[TxQueueTransaction]](uri)

  def transaction(oid: String, userRoot: PayeRoot)(implicit hc: HeaderCarrier): Future[Option[TxQueueTransaction]] =
    httpGetF[TxQueueTransaction](userRoot.transactionLinks.get("findByOid").get.replace("{oid}", oid))

}
