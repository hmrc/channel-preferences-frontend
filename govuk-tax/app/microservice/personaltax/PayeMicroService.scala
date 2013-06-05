package microservice.personaltax

import microservice.{ MicroServiceConfig, MicroService }
import scala.concurrent.{ Await, Future }
import play.api.libs.ws.Response
import controllers.domain.PayeRoot
import microservice.personaltax.domain.TaxCode

class PayeMicroService extends MicroService {

  override val serviceUrl = MicroServiceConfig.payeServiceUrl

  def root(uri: String): PayeRoot = get[PayeRoot](uri)

  def taxCode(uri: String): TaxCode = get[TaxCode](uri)

  def employments(uri: String): Future[Response] = httpResource(uri).get

  private def get[A](uri: String)(implicit m: Manifest[A]): A = Await.result(response[A](httpResource(uri).get), defaultTimeoutDuration)

}
