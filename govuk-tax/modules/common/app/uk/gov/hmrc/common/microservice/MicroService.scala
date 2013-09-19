package uk.gov.hmrc.microservice

import scala.concurrent.duration.Duration
import play.api.libs.ws.{ Response, WS }
import play.api.http.Status
import controllers.common.domain.Transform._
import scala.concurrent.{ Await, ExecutionContext }
import ExecutionContext.Implicits.global
import play.api.{ Logger, Play }
import scala.concurrent.Future
import uk.gov.hmrc.common.microservice.domain.RegimeRoot
import play.api.libs.json.JsValue
import org.slf4j.MDC
import controllers.common.HeaderNames

trait TaxRegimeMicroService[A <: RegimeRoot] extends MicroService {

  def root[A](uri: String)(implicit m: Manifest[A]): A = httpGet[A](uri).getOrElse(throw new IllegalStateException(s"Expected root not found at URI '$uri'"))

  def linkedResource[T](uri: String)(implicit m: Manifest[T]) = {
    Logger.debug(s"Loading linked resource uri: $uri")
    httpGet[T](uri)
  }
}

trait MicroService extends Status with HeaderNames {

  import collection.JavaConversions._

  protected val serviceUrl: String
  protected val success = Statuses(OK to MULTI_STATUS)

  private def headers(): Seq[(String, String)] = {
    val headers = for {
      (k, v) <- MDC.getCopyOfContextMap.toMap.asInstanceOf[Map[String, String]]
    } yield k match {
      case `authorisation` => (k, s"Bearer $v")
      case _ => (k, v)
    }
    headers.toSeq
  }

  protected def httpResource(uri: String) = {
    Logger.info(s"Accessing backend service: $serviceUrl$uri")
    WS.url(s"$serviceUrl$uri").withHeaders(headers(): _*)
  }

  protected case class Statuses(r: Range) {
    def unapply(i: Int): Boolean = r contains i
  }

  protected def httpGet[A](uri: String)(implicit m: Manifest[A]): Option[A] = Await.result(response[A](httpResource(uri).get())(extractJSONResponse[A]), MicroServiceConfig.defaultTimeoutDuration)

  protected def httpPut[A](uri: String, body: JsValue, headers: Map[String, String] = Map.empty)(implicit m: Manifest[A]): Option[A] = {
    val wsResource = httpResource(uri)
    Await.result(response[A](wsResource.withHeaders(headers.toSeq: _*).put(body))(extractJSONResponse[A]), MicroServiceConfig.defaultTimeoutDuration)
  }

  protected def httpPutNoResponse(uri: String, body: JsValue, headers: Map[String, String] = Map.empty) = {
    val wsResource = httpResource(uri)
    Await.result(response(wsResource.withHeaders(headers.toSeq: _*).put(body))(extractNoResponse), MicroServiceConfig.defaultTimeoutDuration)
  }

  protected def httpPost[A](uri: String, body: JsValue, headers: Map[String, String] = Map.empty)(implicit m: Manifest[A]): Option[A] = {
    val wsResource = httpResource(uri)
    Await.result(response[A](wsResource.withHeaders(headers.toSeq: _*).post(body))(extractJSONResponse[A]), MicroServiceConfig.defaultTimeoutDuration)
  }

  protected def httpPostSynchronous(uri: String, body: JsValue, headers: Map[String, String] = Map.empty): Response = {
    val wsResource = httpResource(uri)
    Await.result(wsResource.withHeaders(headers.toSeq: _*).post(body), MicroServiceConfig.defaultTimeoutDuration)
  }

  protected def httpPostAndForget(uri: String, body: JsValue, headers: Map[String, String] = Map.empty) {
    val wsResource = httpResource(uri)
    wsResource.withHeaders(headers.toSeq: _*).post(body)
  }

  protected def httpPutAndForget(uri: String, body: JsValue, headers: Map[String, String] = Map.empty) {
    val wsResource = httpResource(uri)
    wsResource.withHeaders(headers.toSeq: _*).put(body)
  }

  protected def httpDeleteAndForget(uri: String) {
    val wsResource = httpResource(uri)
    wsResource.delete()
  }

  protected def extractJSONResponse[A](response: Response)(implicit m: Manifest[A]): A = {
    try {
      println(response.body)
      fromResponse[A](response.body)
    } catch {
      case e: Throwable => {
        throw new Exception("Malformed result", e)
      }
    }
  }

  protected def extractNoResponse(response: Response): Response = {
    response
  }

  protected def response[A](futureResponse: Future[Response])(handleResponse: (Response) => A)(implicit m: Manifest[A]): Future[Option[A]] = {
    futureResponse map {
      res =>
        res.status match {
          case OK => Some(handleResponse(res))
          //          case success() => //do nothing
          case CREATED => Some(handleResponse(res))
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
  lazy val governmentGatewayServiceUrl = s"$protocol://${Play.configuration.getString(s"govuk-tax.$env.services.government-gateway.host").getOrElse("localhost")}:${Play.configuration.getInt(s"govuk-tax.$env.services.government-gateway.port").getOrElse(8570)}"
  lazy val saServiceUrl = s"$protocol://${Play.configuration.getString(s"govuk-tax.$env.services.sa.host").getOrElse("localhost")}:${Play.configuration.getInt(s"govuk-tax.$env.services.sa.port").getOrElse(8900)}"
  lazy val txQueueUrl = s"$protocol://${Play.configuration.getString(s"govuk-tax.$env.services.txqueue.host").getOrElse("localhost")}:${Play.configuration.getInt(s"govuk-tax.$env.services.txqueue.port").getOrElse(8700)}"
  lazy val auditServiceUrl = s"$protocol://${Play.configuration.getString(s"govuk-tax.$env.services.datastream.host").getOrElse("localhost")}:${Play.configuration.getInt(s"govuk-tax.$env.services.datastream.port").getOrElse(8100)}"
  lazy val keyStoreServiceUrl = s"$protocol://${Play.configuration.getString(s"govuk-tax.$env.services.keystore.host").getOrElse("localhost")}:${Play.configuration.getInt(s"govuk-tax.$env.services.keystore.port").getOrElse(8400)}"
  lazy val agentServiceUrl = s"$protocol://${Play.configuration.getString(s"govuk-tax.$env.services.agent.host").getOrElse("localhost")}:${Play.configuration.getInt(s"govuk-tax.$env.services.agent.port").getOrElse(8420)}"
  lazy val vatServiceUrl = s"$protocol://${Play.configuration.getString(s"govuk-tax.$env.services.vat.host").getOrElse("localhost")}:${Play.configuration.getInt(s"govuk-tax.$env.services.vat.port").getOrElse(8880)}"

  lazy val defaultTimeoutDuration = Duration(Play.configuration.getString(s"$env.services.timeout").getOrElse("30 seconds"))

}

