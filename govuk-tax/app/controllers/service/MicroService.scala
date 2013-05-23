package controllers.service

import play.api.Play
import play.api.libs.ws.{ Response, WS }
import scala.concurrent.Future

private object services {

  import play.api.Play.current

  private val env = Play.mode

  val protocol = Play.configuration.getString(s"$env.services.protocol").getOrElse("http")

  lazy val authServiceUrl = s"$protocol://${Play.configuration.getString(s"govuk-tax.$env.services.auth.host").getOrElse("localhost")}:${Play.configuration.getInt(s"govuk-tax.$env.services.auth.url").getOrElse(8080)}"
  lazy val personServiceUrl = s"$protocol://${Play.configuration.getString(s"govuk-tax.$env.services.person.host").getOrElse("localhost")}:${Play.configuration.getInt(s"govuk-tax.$env.services.person.url").getOrElse(8081)}"
  lazy val companyServiceUrl = s"$protocol://${Play.configuration.getString(s"govuk-tax.$env.services.company.host").getOrElse("localhost")}:${Play.configuration.getInt(s"govuk-tax.$env.services.company.url").getOrElse(8082)}"
  lazy val samlServiceUrl = s"$protocol://${Play.configuration.getString(s"govuk-tax.$env.services.saml.host").getOrElse("localhost")}:${Play.configuration.getInt(s"govuk-tax.$env.services.saml.url").getOrElse(8083)}"
}

trait MicroService {
  val serviceUrl: String

  def resource(uri: String) = WS.url(s"$serviceUrl/$uri")
}

class Auth(override val serviceUrl: String = services.authServiceUrl) extends MicroService {
  def taxUser(pid: String): Future[Response] = resource(s"/auth/pid/$pid").get()
}

class Person(override val serviceUrl: String = services.personServiceUrl) extends MicroService

class Company(override val serviceUrl: String = services.companyServiceUrl) extends MicroService

class Saml(override val serviceUrl: String = services.samlServiceUrl) extends MicroService {
  def samlFormData: Future[Response] = resource("/ida/saml").get
}
