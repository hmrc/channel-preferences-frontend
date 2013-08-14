package uk.gov.hmrc.microservice.sa

import play.Logger
import uk.gov.hmrc.microservice.sa.domain.{ SaRoot, SaPerson }
import uk.gov.hmrc.microservice.{ MicroService, MicroServiceConfig }

class SaMicroService extends MicroService {

  override val serviceUrl = MicroServiceConfig.saServiceUrl

  def root(uri: String): SaRoot = httpGet[SaRoot](uri).getOrElse(throw new IllegalStateException(s"Expected SA root not found at URI '$uri'"))
  def person(uri: String): Option[SaPerson] = httpGet[SaPerson](uri)

  def linkedResource[T](uri: String)(implicit m: Manifest[T]) = {
    Logger.debug(s"Loading linked sa resource, uri: $uri")
    httpGet[T](uri)
  }
}
