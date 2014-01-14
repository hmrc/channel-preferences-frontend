package uk.gov.hmrc

import scala.concurrent.duration.Duration
import play.api.libs.ws.{Response, WS}
import play.api.http.Status
import uk.gov.hmrc.Transform._
import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global
import play.api.{Logger, Play}
import scala.concurrent.Future
import play.api.libs.json.{Json, JsValue}
import com.google.common.net.HttpHeaders
import java.net.URLEncoder
import uk.gov.hmrc.EmailVerificationLinkResponse.EmailVerificationLinkResponse
import scala.Range
import uk.gov.hmrc.common.microservice.Connector
import controllers.common.actions.HeaderCarrier

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

  protected def httpGetF[A](uri: String)(implicit m: Manifest[A]): Future[Option[A]] =
    response[A](httpResource(uri).get())(extractJSONResponse[A])

  protected def httpPostRawF(uri: String, body: JsValue, headers: Map[String, String] = Map.empty): Future[Response] = {
    val wsResource = httpResource(uri)
    wsResource.withHeaders(headers.toSeq: _*).post(body)
  }

  protected def httpPostAndForget(uri: String, body: JsValue, headers: Map[String, String] = Map.empty) {
    val wsResource = httpResource(uri)
    wsResource.withHeaders(headers.toSeq: _*).post(body)
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

  private lazy val services = s"govuk-tax.$env.services"

  lazy val protocol = Play.configuration.getString(s"$services.protocol").getOrElse("http")

  lazy val preferenceServiceUrl = s"$protocol://${Play.configuration.getString(s"$services.preferences.host").getOrElse("localhost")}:${Play.configuration.getInt(s"$services.preferences.port").getOrElse(8900)}"

  lazy val emailServiceUrl = s"$protocol://${Play.configuration.getString(s"$services.email.host").getOrElse("localhost")}:${Play.configuration.getInt(s"$services.email.port").getOrElse(8300)}"

  lazy val defaultTimeoutDuration = Duration(Play.configuration.getString(s"$services.timeout").getOrElse("30 seconds"))

}

class PreferencesConnector extends Connector {

  override val serviceUrl = MicroServiceConfig.preferenceServiceUrl

  def savePreferences(utr: String, digital: Boolean, email: Option[String] = None)(implicit hc: HeaderCarrier) =
    httpPostF(s"/portal/preferences/sa/individual/$utr/print-suppression", Some(Json.parse(toRequestBody(SaPreference(digital, email)))))

  def getPreferences(utr: String)(implicit hc: HeaderCarrier): Future[Option[SaPreference]] =
    httpGetF[SaPreference](s"/portal/preferences/sa/individual/$utr/print-suppression")
      .map(_.orElse(throw new RuntimeException(s"Access to resource: '/portal/preferences/sa/individual/$utr/print-suppression' gave an inconsistent response")))
      .recover { case MicroServiceException(errorMessage, response) if response.status == 404 => None}

  def updateEmailValidationStatus(token: String)(implicit hc: HeaderCarrier): Future[EmailVerificationLinkResponse] = {
    httpPost("/preferences/sa/verify-email", Json.parse(toRequestBody(ValidateEmail(token)))) {
      _.status match {
        case s if OK to MULTI_STATUS contains s => EmailVerificationLinkResponse.OK
        case GONE => EmailVerificationLinkResponse.EXPIRED
        case _ => EmailVerificationLinkResponse.ERROR
      }
    }
  }
}

object EmailVerificationLinkResponse extends Enumeration {
  type EmailVerificationLinkResponse = Value

  val OK, EXPIRED, ERROR = Value
}

class EmailConnector() extends MicroService {

  protected val serviceUrl = MicroServiceConfig.emailServiceUrl

  def validateEmailAddress(emailAddress: String): Future[Boolean] = {
    httpGetF[ValidateEmailResponse](s"/validate-email-address?email=${URLEncoder.encode(emailAddress, "UTF-8")}") map
      (_.getOrElse(throw new RuntimeException(s"Access to resource: '/validate-email-address' gave an invalid response")).valid)
  }
}

case class ValidateEmailResponse(valid: Boolean)

case class ValidateEmail(token: String)

case class SaPreference(digital: Boolean, email: Option[String] = None)

