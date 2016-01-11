package config

import javassist.NotFoundException

import uk.gov.hmrc.circuitbreaker.{UsingCircuitBreaker, CircuitBreakerConfig}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.{Upstream5xxResponse, BadRequestException, Upstream4xxResponse}

trait ServicesCircuitBreaker extends UsingCircuitBreaker { this: ServicesConfig =>

  val externalServiceName: String

  override def circuitBreakerConfig : CircuitBreakerConfig = CircuitBreakerConfig(
    serviceName = externalServiceName,
    numberOfCallsToTriggerStateChange = config(externalServiceName).getInt("circuitBreaker.numberOfCallsToTriggerStateChange"),
    unavailablePeriodDuration = config(externalServiceName).getInt("circuitBreaker.unavailablePeriodDurationInSeconds"),
    unstablePeriodDuration = config(externalServiceName).getInt("circuitBreaker.unstablePeriodDurationInSeconds")
  )

  override protected def breakOnException(t: Throwable): Boolean = t match {
    case (_: Upstream4xxResponse | _: NotFoundException | _: BadRequestException) => false
    case e: Upstream5xxResponse => true
    case _:Throwable => true
  }
}
