package controllers.service

import microservice.auth.AuthMicroService
import microservice.personaltax.PayeMicroService

trait MicroServices {

  val authMicroService = new AuthMicroService()
  val payeMicroService = new PayeMicroService()
}
