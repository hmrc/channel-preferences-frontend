package config

import play.mvc.Http.Status._
import uk.gov.hmrc.circuitbreaker.{CircuitBreakerConfig, UsingCircuitBreaker}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.{Upstream4xxResponse, Upstream5xxResponse}

trait ServicesCircuitBreaker extends UsingCircuitBreaker {
  this: ServicesConfig =>

  protected val externalServiceName: String

  override protected def circuitBreakerConfig = CircuitBreakerConfig(
    serviceName = externalServiceName,
    numberOfCallsToTriggerStateChange = config(externalServiceName).getInt("circuitBreaker.numberOfCallsToTriggerStateChange"),
    unavailablePeriodDuration = config(externalServiceName).getInt("circuitBreaker.unavailablePeriodDurationInSeconds"),
    unstablePeriodDuration = config(externalServiceName).getInt("circuitBreaker.unstablePeriodDurationInSeconds")
  )

  override protected def breakOnException(t: Throwable): Boolean = t match {
    case t: Upstream4xxResponse => t.upstreamResponseCode == BAD_REQUEST
    case _: Upstream5xxResponse => true
    case _                      => true
  }
}
