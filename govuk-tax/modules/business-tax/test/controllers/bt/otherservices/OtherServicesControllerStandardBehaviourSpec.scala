package controllers.bt.otherservices

import uk.gov.hmrc.common.BaseSpec
import play.api.test.{FakeApplication, WithApplication}
import play.api.test.Helpers._
import controllers.bt.testframework.request.{NonBusinessTaxRequest, BusinessTaxRequest, EmptySessionRequest, NoSessionRequest}
import controllers.bt.testframework.fixtures.{JohnDensmoreTestFixture, GeoffFisherTestFixture}
import controllers.bt.OtherServicesController
import uk.gov.hmrc.common.microservice.governmentgateway.GovernmentGatewayConnector


class OtherServicesControllerStandardBehaviourSpec extends BaseSpec {

  val ggw = mock[GovernmentGatewayConnector]
  val controllerUnderTest = new OtherServicesController(ggw, null)(null)


  "Calling otherServices" should {
    "redirect if there is no session" in new WithApplication(FakeApplication()) with NoSessionRequest {
      val result = controllerUnderTest.otherServices(request)
      status(result) shouldBe 303
    }

    "redirect if there is no logged in user" in new WithApplication(FakeApplication()) with EmptySessionRequest {
      val result =  controllerUnderTest.otherServices(request)
      status(result) shouldBe 303
    }

    "redirect if the session has timed out" in new WithApplication(FakeApplication()) with GeoffFisherTestFixture with BusinessTaxRequest {
      override val lastRequestTimestamp = Some(currentTime.minusMinutes(20))
      val result =  controllerUnderTest.otherServices(request)
      status(result) shouldBe 303
    }

    "redirect if the user is not a business tax user" in new WithApplication(FakeApplication()) with NonBusinessTaxRequest with JohnDensmoreTestFixture {
      val result =  controllerUnderTest.otherServices(request)
      status(result) shouldBe 303
      header("Location", result) shouldBe Some("/")
    }
  }
}

