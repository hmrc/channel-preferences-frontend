package microservice

import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit
import play.api.libs.ws.{Response, WS}
import java.net.URI
import play.api.http.Status
import controllers.domain.Transform._
import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global
import play.api.{Logger, Play}

object MicroServiceConfig {

  import play.api.Play.current

  private val env = Play.mode

  val protocol = Play.configuration.getString(s"$env.services.protocol").getOrElse("http")

  lazy val authServiceUrl = s"$protocol://${Play.configuration.getString(s"govuk-tax.$env.services.auth.host").getOrElse("localhost")}:${Play.configuration.getInt(s"govuk-tax.$env.services.auth.port").getOrElse(8080)}"
  lazy val personalTaxServiceUrl = s"$protocol://${Play.configuration.getString(s"govuk-tax.$env.services.person.host").getOrElse("localhost")}:${Play.configuration.getInt(s"govuk-tax.$env.services.person.port").getOrElse(8081)}"
  lazy val companyServiceUrl = s"$protocol://${Play.configuration.getString(s"govuk-tax.$env.services.company.host").getOrElse("localhost")}:${Play.configuration.getInt(s"govuk-tax.$env.services.company.port").getOrElse(8082)}"
  lazy val samlServiceUrl = s"$protocol://${Play.configuration.getString(s"govuk-tax.$env.services.saml.host").getOrElse("localhost")}:${Play.configuration.getInt(s"govuk-tax.$env.services.saml.port").getOrElse(8083)}"
}


trait MicroService extends Status {

  val serviceUrl: String

  protected val defaultTimeoutDuration = Duration(5, TimeUnit.SECONDS)

  def httpResource(uri: String) = {
    Logger.info(s"Accessing backend service: $serviceUrl$uri")
    WS.url(s"$serviceUrl$uri")
  }

  import scala.concurrent.Future

  def getOrNone[T](u: Option[URI])(f: String => Future[T]): Future[T] = {
    u match {
      case Some(uri) => f(uri.toString)
      case None => Future.successful[T](None.asInstanceOf[T])
    }
  }

  case class Statuses(r: Range) {
    def unapply(i: Int): Boolean = r contains i
  }

  val success = Statuses(OK to MULTI_STATUS)

  def response[A](futureResponse: Future[Response])(implicit m: Manifest[A]): Future[A] = {
    futureResponse map {
      res =>
        res.status match {
          case OK => fromResponse[A](res.body)
          //          case success() => //do nothing

          //TODO: add some proper error handling
          case BAD_REQUEST => throw new RuntimeException("Bad request")
          case UNAUTHORIZED => throw new RuntimeException("Unauthenticated request")
          case FORBIDDEN => throw new RuntimeException("Not authorised to make this request")
          case NOT_FOUND => throw new RuntimeException("Resource not found")
          case CONFLICT => throw new RuntimeException("Invalid state")
          case _ => throw new RuntimeException("Internal server error")
        }
    }
  }
}

