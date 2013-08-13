package microservice.txqueue

import microservice.{ MicroService, MicroServiceConfig }
import microservice.sa.domain.SaPerson

/**
 * @author Eric Vlaanderen
 */
class TxQueueMicroService extends MicroService {

  override val serviceUrl = MicroServiceConfig.txQueueUrl

  def transaction(uri: String): Option[List[TxQueueTransaction]] = httpGet[List[TxQueueTransaction]](uri)

}
