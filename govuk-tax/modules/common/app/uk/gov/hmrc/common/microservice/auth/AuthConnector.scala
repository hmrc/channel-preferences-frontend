package uk.gov.hmrc.common.microservice.auth

import uk.gov.hmrc.common.microservice.{MicroServiceException, Connector, MicroServiceConfig}
import uk.gov.hmrc.common.microservice.auth.domain.Authority
import controllers.common.actions.HeaderCarrier
import scala.concurrent.Future

class AuthConnector(override val serviceUrl: String = MicroServiceConfig.authServiceUrl) extends Connector {

  def authority(path: String)(implicit hc: HeaderCarrier) = httpGetF[Authority](path)

  def loginWithPid(pid: String)(implicit hc: HeaderCarrier): Future[Option[Authority]] = httpPostF[Authority, Nothing](s"/auth/pid/$pid", None)

//  def exchangePid(pid: String)(implicit hc: HeaderCarrier): Future[Option[String]] = httpPostF[String, Nothing](s"/auth/pid/$pid/exchange", None)

  def exchangeCredIdForBearerToken(credId: String)(implicit hc: HeaderCarrier): Future[String] = {
    httpPostF[String, Nothing](s"/auth/cred-id/$credId/exchange", None).map {
      case Some(bearerToken) => bearerToken
      case None => throw new RuntimeException(s"No Content or Not Found response when exchanging credId for bearer token")
    }
  }

}
