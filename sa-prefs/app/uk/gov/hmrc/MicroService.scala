package uk.gov.hmrc

import scala.concurrent.duration.Duration
import play.api.libs.ws.{ Response, WS }
import play.api.http.Status
import uk.gov.hmrc.Transform._
import scala.concurrent.{ Await, ExecutionContext }
import ExecutionContext.Implicits.global
import play.api.{ Logger, Play }
import scala.concurrent.Future
import play.api.libs.json.{ Json, JsValue }
import org.slf4j.MDC
import com.google.common.net.HttpHeaders

trait HeaderNames {
  val requestId = "X-Request-ID"
  val authorisation = HttpHeaders.AUTHORIZATION
  val forwardedFor = "x-forwarded-for"
}

trait MicroService extends Status with HeaderNames {

  protected val serviceUrl: String
  protected val success = Statuses(OK to MULTI_STATUS)

  protected def httpResource(uri: String) = {
    Logger.info(s"Accessing backend service: $serviceUrl$uri")
    WS.url(s"$serviceUrl$uri")
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
          case CREATED => Some(handleResponse(res))
          case NO_CONTENT => None
          case NOT_FOUND => throw MicroServiceException("Not Found", res)
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

  lazy val preferenceServiceUrl = s"$protocol://${Play.configuration.getString(s"sa-prefs.$env.services.preferences.host").getOrElse("localhost")}:${Play.configuration.getInt(s"sa-prefs.$env.services.preferences.port").getOrElse(8900)}"

  lazy val defaultTimeoutDuration = Duration(Play.configuration.getString(s"$env.services.timeout").getOrElse("30 seconds"))

}

class SaMicroService extends MicroService {

  override val serviceUrl = MicroServiceConfig.preferenceServiceUrl

  def savePreferences(utr: String, digital: Boolean, email: Option[String] = None) {
    httpPutNoResponse(s"/preferences/sa/utr/$utr/preferences", Json.parse(toRequestBody(SaPreference(digital, email))))
  }

  def getPreferences(utr: String): Option[SaPreference] = {

    try {
      val v = httpGet[SaPreference](s"/preferences/sa/utr/$utr/preferences")
      v.map(Predef.identity).orElse(throw new RuntimeException(s"Access to resource: '/preferences/sa/utr/$utr/preferences' gave an inconsistent response"))

    } catch {
      case MicroServiceException(errorMessage, response) if response.status == 404 => None
      case otherException: Exception => throw otherException
    }
  }
}
case class SaPreference(digital: Boolean, email: Option[String] = None)

