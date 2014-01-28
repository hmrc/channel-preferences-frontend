package uk.gov.hmrc.common.microservice.governmentgateway

import uk.gov.hmrc.common.microservice.{Connector, MicroServiceConfig}
import org.joda.time.DateTime
import controllers.common.actions.HeaderCarrier
import scala.concurrent.Future


class GovernmentGatewayConnector extends Connector {

  override val serviceUrl = MicroServiceConfig.governmentGatewayServiceUrl

  def login(credentials: Credentials)(implicit hc: HeaderCarrier): Future[GovernmentGatewayLoginResponse] = doLogin("/login", credentials)

  def ssoLogin(ssoLoginRequest: SsoLoginRequest)(implicit hc: HeaderCarrier): Future[GovernmentGatewayLoginResponse] = doLogin("/sso-login", ssoLoginRequest)

  def profile(userId: String)(implicit hc: HeaderCarrier) = httpGetF[ProfileResponse](s"/profile$userId").map {
    _.getOrElse(throw new RuntimeException("Could not retrieve user profile from Government Gateway service"))
  }

  private def doLogin[T](path: String, body: T)(implicit m : Manifest[T], hc: HeaderCarrier) =
    httpPostF[GovernmentGatewayLoginResponse,T](path,Some(body)).map(_.getOrElse(throw new IllegalStateException("Expected UserAuthority response but none returned")))
}

case class Credentials(userId: String, password: String)

case class SsoLoginRequest(token: String,  timestamp: Long)

case class GovernmentGatewayLoginResponse(@deprecated("Call auth to exchange credId for authToken", "2014-01-28") authId: String,   // TODO [JJS] REMOVE DEPRECATED CODE
                                          credId: String, name: String, affinityGroup: String, encodedGovernmentGatewayToken: String)

case class ProfileResponse(affinityGroup: String, activeEnrolments: List[String])

object AffinityGroupValue {
  val INDIVIDUAL = "Individual"
  val ORGANISATION = "Organisation"
}
