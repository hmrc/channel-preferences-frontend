package connectors

import config.ServicesCircuitBreaker
import play.api.http.Status
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http._

import scala.concurrent.Future


trait PreferencesConnector extends Status with ServicesCircuitBreaker {

  this: ServicesConfig =>

  val externalServiceName = "preferences"

  def http: HttpGet with HttpPost with HttpPut

  def serviceUrl: String

  def url(path: String) = s"$serviceUrl$path"

  def getPreferencesStatus(taxIdName: String, taxId: String)(implicit headerCarrier: HeaderCarrier): Future[Option[PaperlessPreference]] = {
    withCircuitBreaker(
      http.GET[Option[PaperlessPreference]](url(s"/preferences/$taxIdName/$taxId"))
    )
  }
}

object PreferencesConnector extends PreferencesConnector with ServicesConfig {

  override val serviceUrl = baseUrl("preferences")

  override def http = WsHttp

}


