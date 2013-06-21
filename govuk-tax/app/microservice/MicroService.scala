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

trait TaxRegimeMicroService[A <: RegimeRoot] extends MicroService {
  def root(uri: String): A
}

trait MicroService extends Status {

  protected val serviceUrl: String
  protected val success = Statuses(OK to MULTI_STATUS)
  protected val defaultTimeoutDuration = Duration(5, TimeUnit.SECONDS)

  protected def httpResource(uri: String) = {
    Logger.info(s"Accessing backend service: $serviceUrl$uri")
    WS.url(s"$serviceUrl$uri")
  }

  protected case class Statuses(r: Range) {
    def unapply(i: Int): Boolean = r contains i
  }

  protected def get[A](uri: String)(implicit m: Manifest[A]): Option[A] = Await.result(response[A](httpResource(uri).get()), defaultTimeoutDuration)

  protected def post[A](uri: String, body: JsValue)(implicit m: Manifest[A]): Option[A] = Await.result(response[A](httpResource(uri).post(body)), defaultTimeoutDuration)

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
          case BAD_REQUEST => throw new RuntimeException("Bad request")
          case UNAUTHORIZED => throw new RuntimeException("Unauthenticated request")
          case FORBIDDEN => throw new RuntimeException("Not authorised to make this request")
          case CONFLICT => throw new RuntimeException("Invalid state")
          case x => throw new RuntimeException(s"Internal server error, response status is: $x, futureResponse: $futureResponse")
        }
    }
  }
}

object MicroServiceConfig {

  import play.api.Play.current

  private lazy val env = Play.mode

  lazy val protocol = Play.configuration.getString(s"$env.services.protocol").getOrElse("http")

  lazy val authServiceUrl = s"$protocol://${Play.configuration.getString(s"govuk-tax.$env.services.auth.host").getOrElse("localhost")}:${Play.configuration.getInt(s"govuk-tax.$env.services.auth.port").getOrElse(8500)}"
  lazy val payeServiceUrl = s"$protocol://${Play.configuration.getString(s"govuk-tax.$env.services.personal.host").getOrElse("localhost")}:${Play.configuration.getInt(s"govuk-tax.$env.services.personal.port").getOrElse(8600)}"
  lazy val businessServiceUrl = s"$protocol://${Play.configuration.getString(s"govuk-tax.$env.services.business.host").getOrElse("localhost")}:${Play.configuration.getInt(s"govuk-tax.$env.services.business.port").getOrElse(8510)}"
  lazy val matchingServiceUrl = s"$protocol://${Play.configuration.getString(s"govuk-tax.$env.services.matching.host").getOrElse("localhost")}:${Play.configuration.getInt(s"govuk-tax.$env.services.matching.port").getOrElse(8510)}"
  lazy val samlServiceUrl = s"$protocol://${Play.configuration.getString(s"govuk-tax.$env.services.saml.host").getOrElse("localhost")}:${Play.configuration.getInt(s"govuk-tax.$env.services.saml.port").getOrElse(8540)}"
  lazy val saServiceUrl = s"$protocol://${Play.configuration.getString(s"govuk-tax.$env.services.sa.host").getOrElse("localhost")}:${Play.configuration.getInt(s"govuk-tax.$env.services.sa.port").getOrElse(8900)}"
}

