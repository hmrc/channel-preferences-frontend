package connectors

import config.ServicesCircuitBreaker
import play.api.http.Status
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http._

import scala.concurrent.{ExecutionContext, Future}


trait PreferencesConnector extends Status with ServicesCircuitBreaker {

  this: ServicesConfig =>

  val externalServiceName = "preferences"

  def http: HttpGet with HttpPost with HttpPut

  def serviceUrl: String

  def url(path: String) = s"$serviceUrl$path"

  def getPreferencesStatus(taxIdName: String, taxId: String)(implicit headerCarrier: HeaderCarrier): Future[Option[PaperlessPreference]] = {
    withCircuitBreaker {
      http.GET[Option[PaperlessPreference]](url(s"/preferences/$taxIdName/$taxId"))
    }
  }

  def autoEnrol(preference: PaperlessPreference, taxIdName: String, taxId: String)(implicit headerCarrier: HeaderCarrier, ec: ExecutionContext): Future[Unit] = {
    val maybeBody: Option[JsObject] = for {
      currentEmail <- preference.email
      defaultService <- preference.services.get("default")
      body = Json.parse(
        s"""
           |{
           |	"paperless": {
           |    "terms":"${defaultService.terms}",
           |	  "optedIn":${defaultService.optedIn}
           |   },
           |	"emailAddress":"${currentEmail.address}"
           |}""".stripMargin).as[JsObject]
    } yield body

    maybeBody.fold(Future.successful((): Unit)) {
      case body: JsObject =>
        http.PUT(url(s"/preferences/$taxIdName/$taxId/default"), body)
          .map(_ => (): Unit)
    }
  }
}

object PreferencesConnector extends PreferencesConnector with ServicesConfig {

  override val serviceUrl = baseUrl("preferences")

  override def http = WsHttp

}


