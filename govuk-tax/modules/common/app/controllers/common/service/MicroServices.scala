package controllers.common.service

import uk.gov.hmrc.microservice.auth.AuthMicroService
import uk.gov.hmrc.microservice.paye.PayeMicroService
import uk.gov.hmrc.microservice.saml.SamlMicroService
import uk.gov.hmrc.microservice.sa.SaMicroService
import uk.gov.hmrc.microservice.governmentgateway.GovernmentGatewayMicroService
import uk.gov.hmrc.microservice.txqueue.TxQueueMicroService


trait MicroServices {

  implicit val authMicroService = new AuthMicroService()
  implicit val payeMicroService = new PayeMicroService()
  implicit val samlMicroService = new SamlMicroService()
  implicit val saMicroService = new SaMicroService()
  implicit val governmentGatewayMicroService = new GovernmentGatewayMicroService()
  implicit val txQueueMicroService = new TxQueueMicroService()
}
