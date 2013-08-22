package uk.gov.hmrc.microservice.txqueue

import uk.gov.hmrc.microservice.{ MicroServiceConfig, MicroService }
import uk.gov.hmrc.microservice.paye.domain.PayeRoot

class TxQueueMicroService extends MicroService {

  override val serviceUrl = MicroServiceConfig.txQueueUrl

  def transaction(uri: String): Option[List[TxQueueTransaction]] = httpGet[List[TxQueueTransaction]](uri)
  def transaction(oid: String, userRoot: PayeRoot): Option[TxQueueTransaction] = httpGet[TxQueueTransaction](userRoot.transactionLinks.get("findByOid").get.replace("{oid}", oid))

}
