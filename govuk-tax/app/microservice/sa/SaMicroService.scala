package microservice.sa

import microservice.{ MicroServiceConfig, MicroService }
import microservice.saml.domain.{ AuthResponseValidationResult, AuthRequestFormData }
import play.api.libs.json.Json
import play.Logger
//import microservice.sa.domain.{ SaPerson, Person, DesignatoryDetails, SaRoot }
import microservice.sa.domain.{ SaPerson, SaRoot }

class SaMicroService extends MicroService {

  override val serviceUrl = MicroServiceConfig.saServiceUrl

  def root(uri: String): SaRoot = httpGet[SaRoot](uri).getOrElse(throw new IllegalStateException(s"Expected SA root not found at URI '$uri'"))
  def person(uri: String): Option[SaPerson] = httpGet[SaPerson](uri)

  def linkedResource[T](uri: String)(implicit m: Manifest[T]) = {
    Logger.debug(s"Loading linked sa resource, uri: $uri")
    httpGet[T](uri)
  }
}
