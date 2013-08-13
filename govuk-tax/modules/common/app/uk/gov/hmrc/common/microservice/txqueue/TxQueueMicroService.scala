package uk.gov.hmrc.microservice.txqueue

import uk.gov.hmrc.microservice.{ MicroServiceConfig, MicroService }

class TxQueueMicroService extends MicroService {

  override val serviceUrl = MicroServiceConfig.txQueueUrl

  def transaction(uri: String): Option[List[TxQueueTransaction]] = httpGet[List[TxQueueTransaction]](uri)

}
