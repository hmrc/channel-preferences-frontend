package microservice

import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit
import play.api.libs.ws.{ Response, WS }
import play.api.http.Status
import controllers.domain.Transform._
import scala.concurrent.{ Await, ExecutionContext }
import ExecutionContext.Implicits.global
import play.api.{ Logger, Play }
import scala.concurrent.Future
import microservice.domain.RegimeRoot
import play.api.libs.json.JsValue
import org.slf4j.MDC
import play.api.libs.ws.WS.WSRequestHolder
import controllers.HeaderNames

trait TaxRegimeMicroService[A <: RegimeRoot] extends MicroService {

  def root[A](uri: String)(implicit m: Manifest[A]): A = httpGet[A](uri).getOrElse(throw new IllegalStateException(s"Expected root not found at URI '$uri'"))

  def linkedResource[T](uri: String)(implicit m: Manifest[T]) = {
    Logger.debug(s"Loading linked resource uri: $uri")
    httpGet[T](uri)
  }
}

trait MicroService extends Status with HeaderNames {

  protected val serviceUrl: String
  protected val success = Statuses(OK to MULTI_STATUS)

  private def setHeaders(client: WSRequestHolder): WSRequestHolder = {
    val requestHolder = if (Option(MDC.get(authorisation)).isDefined) client.withHeaders((authorisation, s"Bearer ${MDC.get(authorisation)}")) else client
    if (Option(MDC.get(requestId)).isDefined) requestHolder.withHeaders((requestId, MDC.get(requestId))) else requestHolder
  }

  protected def httpResource(uri: String) = {
    Logger.info(s"Accessing backend service: $serviceUrl$uri")
    setHeaders(WS.url(s"$serviceUrl$uri"))
  }

  protected case class Statuses(r: Range) {
    def unapply(i: Int): Boolean = r contains i
  }

  protected def httpGet[A](uri: String)(implicit m: Manifest[A]): Option[A] = Await.result(response[A](httpResource(uri).get()), MicroServiceConfig.defaultTimeoutDuration)

  protected def httpPost[A](uri: String, body: JsValue, headers: Map[String, String] = Map.empty)(implicit m: Manifest[A]): Option[A] = {
    val wsResource = httpResource(uri)
    Await.result(response[A](wsResource.withHeaders(headers.toSeq: _*).post(body)), MicroServiceConfig.defaultTimeoutDuration)
  }

  protected def response[A](futureResponse: Future[Response])(implicit m: Manifest[A]): Future[Option[A]] = {
    futureResponse map {
      res =>
        res.status match {
          case OK => Some(fromResponse[A](res.body))
          //          case success() => //do nothing

          //TODO: add some proper error handling
          // 204 or 404 are returned to the micro service as None
          case NO_CONTENT => None
          case NOT_FOUND => None
          case BAD_REQUEST => throw MicroServiceException("Bad request", res)
          case UNAUTHORIZED => throw UnauthorizedException("Unauthenticated request", res)
          case FORBIDDEN => throw MicroServiceException("Not authorised to make this request", res)
          case CONFLICT => throw MicroServiceException("Invalid state", res)
          case x => throw MicroServiceException(s"Internal server error, response status is: $x", res)
        }
    }
  }
}

trait HasResponse {
  val response: Response
}

case class MicroServiceException(message: String, response: Response) extends RuntimeException(message) with HasResponse

case class UnauthorizedException(message: String, response: Response) extends RuntimeException(message) with HasResponse

object MicroServiceConfig {

  import play.api.Play.current

  private lazy val env = Play.mode

  lazy val protocol = Play.configuration.getString(s"$env.services.protocol").getOrElse("http")

  lazy val authServiceUrl = s"$protocol://${Play.configuration.getString(s"govuk-tax.$env.services.auth.host").getOrElse("localhost")}:${Play.configuration.getInt(s"govuk-tax.$env.services.auth.port").getOrElse(8500)}"

  lazy val payeServiceUrl = s"$protocol://${Play.configuration.getString(s"govuk-tax.$env.services.paye.host").getOrElse("localhost")}:${Play.configuration.getInt(s"govuk-tax.$env.services.paye.port").getOrElse(8600)}"
  lazy val businessServiceUrl = s"$protocol://${Play.configuration.getString(s"govuk-tax.$env.services.business.host").getOrElse("localhost")}:${Play.configuration.getInt(s"govuk-tax.$env.services.business.port").getOrElse(8510)}"
  lazy val matchingServiceUrl = s"$protocol://${Play.configuration.getString(s"govuk-tax.$env.services.matching.host").getOrElse("localhost")}:${Play.configuration.getInt(s"govuk-tax.$env.services.matching.port").getOrElse(8510)}"
  lazy val samlServiceUrl = s"$protocol://${Play.configuration.getString(s"govuk-tax.$env.services.saml.host").getOrElse("localhost")}:${Play.configuration.getInt(s"govuk-tax.$env.services.saml.port").getOrElse(8540)}"
  lazy val governmentGatewayServiceUrl = s"$protocol://${Play.configuration.getString(s"govuk-tax.$env.services.governmentgateway.host").getOrElse("localhost")}:${Play.configuration.getInt(s"govuk-tax.$env.services.governmentgateway.port").getOrElse(8570)}"
  lazy val saServiceUrl = s"$protocol://${Play.configuration.getString(s"govuk-tax.$env.services.sa.host").getOrElse("localhost")}:${Play.configuration.getInt(s"govuk-tax.$env.services.sa.port").getOrElse(8900)}"

  lazy val defaultTimeoutDuration = Duration(Play.configuration.getString(s"$env.services.timeout").getOrElse("30 seconds"))

}

