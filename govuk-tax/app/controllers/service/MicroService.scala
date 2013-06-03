package controllers.service

import play.api.Play
import play.api.libs.ws.{ Response, WS }
import scala.concurrent.Future
import java.net.URI

private object services {

  import play.api.Play.current

  private val env = Play.mode

  val protocol = Play.configuration.getString(s"$env.services.protocol").getOrElse("http")

  lazy val authServiceUrl = s"$protocol://${Play.configuration.getString(s"govuk-tax.$env.services.auth.host").getOrElse("localhost")}:${Play.configuration.getInt(s"govuk-tax.$env.services.auth.port").getOrElse(8080)}"
  lazy val personalTaxServiceUrl = s"$protocol://${Play.configuration.getString(s"govuk-tax.$env.services.person.host").getOrElse("localhost")}:${Play.configuration.getInt(s"govuk-tax.$env.services.person.port").getOrElse(8081)}"
  lazy val companyServiceUrl = s"$protocol://${Play.configuration.getString(s"govuk-tax.$env.services.company.host").getOrElse("localhost")}:${Play.configuration.getInt(s"govuk-tax.$env.services.company.port").getOrElse(8082)}"
  lazy val samlServiceUrl = s"$protocol://${Play.configuration.getString(s"govuk-tax.$env.services.saml.host").getOrElse("localhost")}:${Play.configuration.getInt(s"govuk-tax.$env.services.saml.port").getOrElse(8083)}"
}

trait MicroService {
  val serviceUrl: String

  def resource(uri: String) = WS.url(s"$serviceUrl$uri")

  def getOrNone[T](u: Option[URI])(f: String => Future[T]): Future[T] = {
    u match {
      case Some(uri) => f(uri.toString)
      case None => Future.successful[T](None.asInstanceOf[T])
    }
  }
}

class Auth(override val serviceUrl: String = services.authServiceUrl) extends MicroService {
  def authority(uri: String): Future[Response] = resource(uri).get()
}

class Personal extends MicroService {
  override val serviceUrl = services.personalTaxServiceUrl
  def personal(uri: String): Future[Response] = resource(uri).get
  def employments(uri: String): Future[Response] = resource(uri).get
}

class Company(override val serviceUrl: String = services.companyServiceUrl) extends MicroService

class Saml(override val serviceUrl: String = services.samlServiceUrl) extends MicroService {
  def samlFormData: Future[Response] = resource("/saml/create").get
}
