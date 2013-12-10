package uk.gov.hmrc.common.microservice.governmentgateway

import play.api.libs.json._
import scala.collection.Seq
import uk.gov.hmrc.microservice.{Connector, MicroServiceConfig}
import org.joda.time.DateTime
import controllers.common.actions.HeaderCarrier
import scala.concurrent.ExecutionContext.Implicits.global


class GovernmentGatewayConnector extends Connector {

  override val serviceUrl = MicroServiceConfig.governmentGatewayServiceUrl

  implicit object CredentialsWrites extends Writes[Credentials] {
    def writes(c: Credentials) = JsObject(Seq("userId" -> JsString(c.userId), "password" -> JsString(c.password)))
  }

  implicit object SsoLoginWrites extends Writes[SsoLoginRequest] {
    def writes(g: SsoLoginRequest) = JsObject(Seq("token" -> JsString(g.token), "timestamp" -> JsNumber(g.timestamp)))
  }

  def login(credentials: Credentials)(implicit hc: HeaderCarrier) = doLogin("/login", credentials)

  def ssoLogin(ssoLoginRequest: SsoLoginRequest)(implicit hc: HeaderCarrier) = doLogin("/sso-login", ssoLoginRequest)

  private def doLogin[T](path: String, body: T)(implicit hc: HeaderCarrier, write: Writes[T]) =
    httpPostF[GovernmentGatewayResponse](
      uri = path,
      body = Json.toJson(body),
      headers = Map.empty
    ).map(_.getOrElse(throw new IllegalStateException("Expected UserAuthority response but none returned")))

  def profile(userId: String)(implicit hc: HeaderCarrier) =
    httpGetF[ProfileResponse](s"/profile$userId").map {
      _.getOrElse(throw new RuntimeException("Could not retrieve user profile from Government Gateway service"))
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


case class GatewayToken(encodeBase64: String, created: DateTime, expires: DateTime)

case class ProfileResponse(affinityGroup: String, activeEnrolments: List[String])

object AffinityGroupValue {
  val INDIVIDUAL = "Individual"
  val ORGANISATION = "Organisation"
  val AGENT = "Agent"
}
