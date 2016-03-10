package config

import uk.gov.hmrc.circuitbreaker.{CircuitBreakerConfig, UsingCircuitBreaker}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.{BadRequestException, NotFoundException, Upstream4xxResponse, Upstream5xxResponse}

trait ServicesCircuitBreaker extends UsingCircuitBreaker {
  this: ServicesConfig =>

  protected val externalServiceName: String

  override protected def circuitBreakerConfig = CircuitBreakerConfig(
    serviceName = externalServiceName,
    numberOfCallsToTriggerStateChange = config(externalServiceName).getInt("circuitBreaker.numberOfCallsToTriggerStateChange"),
    unavailablePeriodDuration = config(externalServiceName).getInt("circuitBreaker.unavailablePeriodDurationInSeconds") map (_ * 1000),
    unstablePeriodDuration = config(externalServiceName).getInt("circuitBreaker.unstablePeriodDurationInSeconds") map (_ * 1000)
  )

  override protected def breakOnException(t: Throwable): Boolean = t match {
    case t: BadRequestException => false
    case t: NotFoundException =>   false
    case t: Upstream4xxResponse => false
    case _: Upstream5xxResponse => true
    case _                      => true
  }
}
