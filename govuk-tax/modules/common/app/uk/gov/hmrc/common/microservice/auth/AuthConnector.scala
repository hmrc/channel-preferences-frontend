package uk.gov.hmrc.common.microservice.auth

import uk.gov.hmrc.common.microservice.{MicroServiceException, Connector, MicroServiceConfig}
import uk.gov.hmrc.common.microservice.auth.domain.Authority
import controllers.common.actions.HeaderCarrier
import scala.concurrent.Future
import controllers.common.{AuthExchangeResponse, AuthToken}

class AuthConnector(override val serviceUrl: String = MicroServiceConfig.authServiceUrl) extends Connector {

  def currentAuthority(implicit hc: HeaderCarrier) = httpGetF[Authority](s"/auth/authority")

  //def loginWithPid(pid: String)(implicit hc: HeaderCarrier): Future[Option[Authority]] = httpPostF[Authority, Nothing](s"/auth/pid/$pid", None)

//  def exchangePid(pid: String)(implicit hc: HeaderCarrier): Future[Option[String]] = httpPostF[String, Nothing](s"/auth/pid/$pid/exchange", None)

  /**
   * This does not update login time (it was already updated
   * (both in the Tax Platform and the Portal) in the Government Gateway Microservice)
   */
  def exchangeCredIdForBearerToken(credId: String)(implicit hc: HeaderCarrier): Future[AuthExchangeResponse] = {
    httpPostF[AuthExchangeResponse, Nothing](s"/auth/cred-id/$credId/exchange", None).map {
      case Some(authExchangeResponse) => authExchangeResponse
      case None => throw AuthTokenExchangeException("credId")
    }
  }

  /**
   * This does not update login time (it was already updated
   * (both in the Tax Platform and the Portal) in the Government Gateway Microservice)
   */
  def exchangePidForBearerToken(pid: String)(implicit hc: HeaderCarrier): Future[AuthExchangeResponse] = {
    httpPostF[AuthExchangeResponse, Nothing](s"/auth/pid/$pid/exchange", None).map {
      case Some(authExchangeResponse) => authExchangeResponse
      case None => throw AuthTokenExchangeException("pid")
    }
  }

}

case class AuthTokenExchangeException(idType: String) extends RuntimeException(s"Unable to exchange $idType for an AuthToken")
