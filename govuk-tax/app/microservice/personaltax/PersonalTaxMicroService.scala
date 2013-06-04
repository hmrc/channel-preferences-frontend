package microservice.personaltax

import microservice.{MicroServiceConfig, MicroService}
import scala.concurrent.Future
import play.api.libs.ws.Response
import domain.paye.PayeRoot

class PayeMicroService extends MicroService {
  override val serviceUrl = MicroServiceConfig.personalTaxServiceUrl

  def root(uri: String): Future[PayeRoot] = response[PayeRoot](httpResource(uri).get)

  def employments(uri: String): Future[Response] = httpResource(uri).get
}