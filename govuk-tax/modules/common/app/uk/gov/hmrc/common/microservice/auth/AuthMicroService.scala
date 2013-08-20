package uk.gov.hmrc.microservice.auth

import uk.gov.hmrc.microservice.{ MicroService, MicroServiceConfig }
import uk.gov.hmrc.microservice.auth.domain.UserAuthority
import uk.gov.hmrc.common.microservice.auth.domain.Preferences

class AuthMicroService(override val serviceUrl: String = MicroServiceConfig.authServiceUrl) extends MicroService {

  import controllers.common.domain.Transform._
  import play.api.libs.json.Json

  def authority(path: String) = httpGet[UserAuthority](path)

  def preferences(credId: String) = httpGet[Preferences](s"/auth/cred-id/${credId}/preferences")
  def savePreferences(oid: String, preferences: Preferences) = httpPutNoResponse(s"${oid}/preferences", Json.parse(toRequestBody(preferences)), Map.empty)
}
