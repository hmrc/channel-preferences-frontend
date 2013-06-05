package microservice.personaltax

import microservice.{ MicroServiceConfig, MicroService }
import scala.concurrent.{ Await, Future }
import play.api.libs.ws.Response
import controllers.domain.PayeRoot

class PayeMicroService extends MicroService {

  override val serviceUrl = MicroServiceConfig.payeServiceUrl

  def root(uri: String): PayeRoot = Await.result(response[PayeRoot](httpResource(uri).get), defaultTimeoutDuration)

  def employments(uri: String): Future[Response] = httpResource(uri).get
}