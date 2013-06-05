package controllers.service

import microservice.{ MicroServiceConfig, MicroService }

class Company(override val serviceUrl: String = MicroServiceConfig.companyServiceUrl) extends MicroService

