package uk.gov.hmrc.common.microservice

import scala.concurrent.duration.Duration
import play.api.libs.ws.{Response, WS}
import play.api.http.Status
import play.api.{Logger, Play}
import scala.concurrent._
import uk.gov.hmrc.common.microservice.domain.RegimeRoot
import controllers.common.HeaderNames
import controllers.common.actions.{LoggingDetails, HeaderCarrier}
import play.api.libs.json.{JsNull, JsValue}
import uk.gov.hmrc.common.MdcLoggingExecutionContext

trait TaxRegimeConnector[A <: RegimeRoot[_]] extends Connector {
  def linkedResource[T](uri: String)(implicit m: Manifest[T], headerCarrier: HeaderCarrier) = {
    Logger.debug(s"Loading linked resource uri: $uri")
    httpGetF[T](uri)
  }
}

trait Connector extends Status with ConnectionLogging {

  import play.api.libs.json.Json
  import controllers.common.domain.Transform._

  protected implicit def fromLoggingDetails(implicit loggingDetails: LoggingDetails) = MdcLoggingExecutionContext.fromLoggingDetails

  protected val serviceUrl: String

  protected def httpResource(uri: String)(implicit headerCarrier: HeaderCarrier) = WS.url(s"$serviceUrl$uri").withHeaders(headerCarrier.headers: _*)

  protected def httpGetF[A](uri: String)(implicit m: Manifest[A], headerCarrier: HeaderCarrier): Future[Option[A]] = withLogging("GetF", uri) {
    response[A](httpResource(uri).get(), uri)(extractJSONResponse[A])
  }

  protected def httpPutF[A, B](uri: String, body: A, headers: Map[String, String] = Map.empty)(implicit a: Manifest[A], b: Manifest[B], headerCarrier: HeaderCarrier): Future[Option[B]] = {
    val wsResource = httpResource(uri)
    response[B](wsResource.withHeaders(headers.toSeq: _*).put(transform[A](Some(body))), uri)(extractJSONResponse[B])
  }

  protected def httpPostF[TResult, TBody](uri: String, body: Option[TBody], headers: Map[String, String] = Map.empty)
                                         (implicit bodyManifest: Manifest[TBody], resultManifest: Manifest[TResult], headerCarrier: HeaderCarrier): Future[Option[TResult]] = {
    response[TResult](httpResource(uri).withHeaders(headers.toSeq: _*).post(transform[TBody](body)), uri)(extractJSONResponse[TResult])
  }

  protected def httpPost[TBody, TResult](uri: String, body: TBody, headers: Map[String, String] = Map.empty)(responseProcessor: (Response => TResult))
                              (implicit bodyManifest: Manifest[TBody], b: Manifest[TResult], headerCarrier: HeaderCarrier): Future[TResult] = {
    httpResource(uri).withHeaders(headers.toSeq: _*).post(transform[TBody](Some(body))).map(responseProcessor)
  }

  protected def httpPostResponse[A](uri: String, body: A, headers: Map[String, String] = Map.empty)(implicit m: Manifest[A], hc: HeaderCarrier): Future[Response] = {
    val wsResource = httpResource(uri)
    wsResource.withHeaders(headers.toSeq: _*).post(transform(Some(body)))
  }

  protected def httpDeleteAndForget(uri: String)(implicit hc: HeaderCarrier) {
    val wsResource = httpResource(uri)
    wsResource.delete() onFailure {
      case throwable =>
        Logger.error(s"Async delete to $uri failed", throwable)
    }
  }

  protected def extractJSONResponse[A](response: Response)(implicit m: Manifest[A]): A = {
    try {
      fromResponse[A](response.body)
    } catch {
      case e: Throwable =>
        throw new Exception("Malformed result", e)
    }
  }

  protected def extractNoResponse(response: Response): Response = {
    response
  }

  protected def response[A](futureResponse: Future[Response], uri: String)(handleResponse: (Response) => A)(implicit m: Manifest[A], hc: HeaderCarrier): Future[Option[A]] = {
    futureResponse map {
      res =>
        res.status match {
          case OK => Some(handleResponse(res))
          case CREATED => Some(handleResponse(res))
          //TODO: add some proper error handling - 204 or 404 are returned as None
          case NO_CONTENT => None
          case NOT_FOUND => None
          case BAD_REQUEST => throw MicroServiceException(s"Bad request trying to hit: ${httpResource(uri)}", res)
          case UNAUTHORIZED => throw UnauthorizedException(s"Unauthenticated request trying to hit: ${httpResource(uri)}", res)
          case FORBIDDEN => throw ForbiddenException(s"Not authorised to make this request trying to hit: ${httpResource(uri)}", res)
          case CONFLICT => throw MicroServiceException(s"Invalid state trying to hit: ${httpResource(uri)}", res)
          case x => throw MicroServiceException(s"Internal server error, response status is: $x trying to hit: ${httpResource(uri)}", res)
        }
    }
  }

  private def transform[TBody](body: Option[TBody]): JsValue = body.map(body => Json.parse(toRequestBody(body))).getOrElse(JsNull)
}

trait HasResponse {
  val response: Response
}

case class MicroServiceException(message: String, response: Response) extends RuntimeException(message) with HasResponse

case class UnauthorizedException(message: String, response: Response) extends RuntimeException(message) with HasResponse

case class ForbiddenException(message: String, response: Response) extends RuntimeException(message) with HasResponse

object MicroServiceConfig {

  import play.api.Play.current

  private lazy val env = Play.mode

  lazy val protocol = Play.configuration.getString(s"$env.services.protocol").getOrElse("http")

  lazy val authServiceUrl = s"$protocol://${Play.configuration.getString(s"govuk-tax.$env.services.auth.host").getOrElse("localhost")}:${Play.configuration.getInt(s"govuk-tax.$env.services.auth.port").getOrElse(8500)}"

  lazy val payeServiceUrl = s"$protocol://${Play.configuration.getString(s"govuk-tax.$env.services.paye.host").getOrElse("localhost")}:${Play.configuration.getInt(s"govuk-tax.$env.services.paye.port").getOrElse(8600)}"
  lazy val businessServiceUrl = s"$protocol://${Play.configuration.getString(s"govuk-tax.$env.services.business.host").getOrElse("localhost")}:${Play.configuration.getInt(s"govuk-tax.$env.services.business.port").getOrElse(8510)}"
  lazy val matchingServiceUrl = s"$protocol://${Play.configuration.getString(s"govuk-tax.$env.services.matching.host").getOrElse("localhost")}:${Play.configuration.getInt(s"govuk-tax.$env.services.matching.port").getOrElse(8510)}"

  lazy val idaTokenApiPathBase = Play.configuration.getString(s"govuk-tax.$env.services.ida.tokenapi.pathBase").getOrElse("http://localhost:8080/ida")
  lazy val idaTokenRequired = Play.configuration.getBoolean(s"govuk-tax.$env.services.ida.tokenapi.tokenRequired").getOrElse(false)
  lazy val idaTokenApiUser = Play.configuration.getString(s"govuk-tax.$env.services.ida.tokenapi.username")
  lazy val idaTokenApiPass = Play.configuration.getString(s"govuk-tax.$env.services.ida.tokenapi.password")

  lazy val samlServiceUrl = s"$protocol://${Play.configuration.getString(s"govuk-tax.$env.services.saml.host").getOrElse("localhost")}:${Play.configuration.getInt(s"govuk-tax.$env.services.saml.port").getOrElse(8540)}"
  lazy val governmentGatewayServiceUrl = s"$protocol://${Play.configuration.getString(s"govuk-tax.$env.services.government-gateway.host").getOrElse("localhost")}:${Play.configuration.getInt(s"govuk-tax.$env.services.government-gateway.port").getOrElse(8570)}"
  lazy val saServiceUrl = s"$protocol://${Play.configuration.getString(s"govuk-tax.$env.services.sa.host").getOrElse("localhost")}:${Play.configuration.getInt(s"govuk-tax.$env.services.sa.port").getOrElse(8900)}"
  lazy val txQueueServiceUrl = s"$protocol://${Play.configuration.getString(s"govuk-tax.$env.services.txqueue.host").getOrElse("localhost")}:${Play.configuration.getInt(s"govuk-tax.$env.services.txqueue.port").getOrElse(8700)}"
  lazy val auditServiceUrl = s"$protocol://${Play.configuration.getString(s"govuk-tax.$env.services.datastream.host").getOrElse("localhost")}:${Play.configuration.getInt(s"govuk-tax.$env.services.datastream.port").getOrElse(8100)}"
  lazy val keyStoreServiceUrl = s"$protocol://${Play.configuration.getString(s"govuk-tax.$env.services.keystore.host").getOrElse("localhost")}:${Play.configuration.getInt(s"govuk-tax.$env.services.keystore.port").getOrElse(8400)}"
  lazy val vatServiceUrl = s"$protocol://${Play.configuration.getString(s"govuk-tax.$env.services.vat.host").getOrElse("localhost")}:${Play.configuration.getInt(s"govuk-tax.$env.services.vat.port").getOrElse(8880)}"
  lazy val epayeServiceUrl = s"$protocol://${Play.configuration.getString(s"govuk-tax.$env.services.epaye.host").getOrElse("localhost")}:${Play.configuration.getInt(s"govuk-tax.$env.services.epaye.port").getOrElse(8880)}"
  lazy val ctServiceUrl = s"$protocol://${Play.configuration.getString(s"govuk-tax.$env.services.ct.host").getOrElse("localhost")}:${Play.configuration.getInt(s"govuk-tax.$env.services.ct.port").getOrElse(8880)}"
  lazy val preferencesServiceUrl = s"$protocol://${Play.configuration.getString(s"govuk-tax.$env.services.preferences.host").getOrElse("localhost")}:${Play.configuration.getInt(s"govuk-tax.$env.services.preferences.port").getOrElse(8025)}"
  lazy val emailServiceUrl = s"$protocol://${Play.configuration.getString(s"govuk-tax.$env.services.email.host").getOrElse("localhost")}:${Play.configuration.getInt(s"govuk-tax.$env.services.email.port").getOrElse(8300)}"
  lazy val hmrcDeskproServiceUrl = s"$protocol://${Play.configuration.getString(s"govuk-tax.$env.services.hmrc-deskpro.host").getOrElse("localhost")}:${Play.configuration.getInt(s"govuk-tax.$env.services.hmrc-deskpro.port").getOrElse(8650)}"

  lazy val defaultTimeoutDuration = Duration(Play.configuration.getString(s"$env.services.timeout").getOrElse("30 seconds"))

}

