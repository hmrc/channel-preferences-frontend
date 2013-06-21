package controllers.service

import microservice.auth.AuthMicroService
import microservice.paye.PayeMicroService
import microservice.saml.SamlMicroService
import microservice.sa.SaMicroService

trait MicroServices {

  implicit val authMicroService = new AuthMicroService()
  implicit val payeMicroService = new PayeMicroService()
  implicit val samlMicroService = new SamlMicroService()
  implicit val saMicroService = new SaMicroService()
}
