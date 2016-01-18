package config

import play.mvc.Http.Status._
import uk.gov.hmrc.circuitbreaker.{CircuitBreakerConfig, UsingCircuitBreaker}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.{Upstream4xxResponse, Upstream5xxResponse}

trait ServicesCircuitBreaker extends UsingCircuitBreaker {
  this: ServicesConfig =>

  val externalServiceName: String

  override def circuitBreakerConfig = CircuitBreakerConfig(
    serviceName = externalServiceName,
    numberOfCallsToTriggerStateChange = config(externalServiceName).getInt("circuitBreaker.numberOfCallsToTriggerStateChange"),
    unavailablePeriodDuration = config(externalServiceName).getInt("circuitBreaker.unavailablePeriodDurationInSeconds"),
    unstablePeriodDuration = config(externalServiceName).getInt("circuitBreaker.unstablePeriodDurationInSeconds")
  )

  override protected def breakOnException(t: Throwable): Boolean = t match {
    case t: Upstream4xxResponse if t.httpCodeIsNot(BAD_REQUEST, UNAUTHORIZED, FORBIDDEN) => false
    case _: Upstream5xxResponse => true
    case _ => true
  }

  private implicit class Upstream4xxResponseExtension(ex: Upstream4xxResponse) {
    def httpCodeIsNot(statuses: Int*): Boolean = statuses.contains(ex.upstreamResponseCode)
  }
}
