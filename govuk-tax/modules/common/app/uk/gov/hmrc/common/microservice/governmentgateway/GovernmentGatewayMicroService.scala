package uk.gov.hmrc.common.microservice.governmentgateway

import play.api.libs.json._
import scala.collection.Seq
import uk.gov.hmrc.microservice.{MicroService, MicroServiceConfig}
import org.joda.time.DateTime

class GovernmentGatewayMicroService extends MicroService {

  override val serviceUrl = MicroServiceConfig.governmentGatewayServiceUrl

  implicit object CredentialsWrites extends Writes[Credentials] {
    def writes(c: Credentials) = JsObject(Seq("userId" -> JsString(c.userId), "password" -> JsString(c.password)))
  }

  implicit object SsoLoginWrites extends Writes[SsoLoginRequest] {
    def writes(g: SsoLoginRequest) = JsObject(Seq("token" -> JsString(g.token), "timestamp" -> JsNumber(g.timestamp)))
  }

  def login(credentials: Credentials) = {
    val loginResponse = httpPost[GovernmentGatewayResponse](
      uri = "/login",
      body = Json.toJson(credentials),
      headers = Map.empty)

    loginResponse.getOrElse(throw new IllegalStateException("Expected UserAuthority response but none returned"))
  }

  def ssoLogin(ssoLoginRequest: SsoLoginRequest) = {
    val ssoResponse = httpPost[GovernmentGatewayResponse](
      uri = "/sso-login",
      body = Json.toJson(ssoLoginRequest),
      headers = Map.empty)

    ssoResponse.getOrElse(throw new IllegalStateException("Expected UserAuthority response but none returned"))
  }

  def profile(userId: String) = {
    val response = httpGet[JsonProfileResponse](s"/profile$userId")
    response match {
      case Some(profile) => Some(ProfileResponse(profile))
      case _ => None
    }
  }
}

case class Credentials(userId: String,
                       password: String)

case class SsoLoginRequest(token: String,
                           timestamp: Long)

case class GovernmentGatewayResponse(authId: String,
                                     name: String,
                                     affinityGroup: String,
                                     encodedGovernmentGatewayToken: GatewayToken)


case class GatewayToken(encodeBase64: String, created : DateTime, expires: DateTime)


case class AffinityGroup(identifier: String)

case class Enrolment(key: String)

case class ProfileResponse(affinityGroup: AffinityGroup, activeEnrolments: Set[Enrolment])

object ProfileResponse {

  def apply(jsonProfileResponse: JsonProfileResponse): ProfileResponse = {
    ProfileResponse(AffinityGroup(jsonProfileResponse.affinityGroup.toLowerCase), jsonProfileResponse.activeEnrolments.map(enrolment => Enrolment(enrolment)))
  }

}

private[governmentgateway] case class JsonProfileResponse(affinityGroup: String, activeEnrolments: Set[String])

object AffinityGroupValue {
  val INDIVIDUAL = "Individual"
  val ORGANISATION = "Organisation"
  val AGENT = "Agent"
}
