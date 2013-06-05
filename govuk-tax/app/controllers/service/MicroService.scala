package controllers.service

import play.api.libs.ws.Response
import scala.concurrent.Future
import microservice.{ MicroServiceConfig, MicroService }

class Company(override val serviceUrl: String = MicroServiceConfig.companyServiceUrl) extends MicroService

class Saml(override val serviceUrl: String = MicroServiceConfig.samlServiceUrl) extends MicroService {
  def samlFormData: Future[Response] = httpResource("/saml/create").get
}
