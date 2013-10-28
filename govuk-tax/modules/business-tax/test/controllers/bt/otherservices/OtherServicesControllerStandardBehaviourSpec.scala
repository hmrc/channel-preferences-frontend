package controllers.bt.otherservices

import uk.gov.hmrc.common.BaseSpec
import play.api.mvc.{AnyContent, Action}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import controllers.bt.testframework.request.{NonBusinessTaxRequest, BusinessTaxRequest, EmptySessionRequest, NoSessionRequest}
import controllers.bt.testframework.fixtures.{JohnDensmoreTestFixture, GeoffFisherTestFixture}
import controllers.bt.OtherServicesController

trait OtherServicesControllerBehaviours extends BaseSpec {

  def aBusinessUserSessionValidatingMethod(method: OtherServicesController => Action[AnyContent]) {

    "redirect if there is no session" in new OtherServicesControllerForTest with NoSessionRequest {
      val result = method(controllerUnderTest)(FakeRequest())
      status(result) shouldBe 303
    }

    "redirect if there is no logged in user" in new OtherServicesControllerForTest with EmptySessionRequest {
      val result =  method(controllerUnderTest)(request)
      status(result) shouldBe 303
    }

    "redirect if the session has timed out" in new OtherServicesControllerForTest with GeoffFisherTestFixture with BusinessTaxRequest {
      override val lastRequestTimestamp = Some(currentTime.minusMinutes(20))
      val result =  method(controllerUnderTest)(request)
      status(result) shouldBe 303
    }

    "redirect if the user is not a business tax user" in new OtherServicesControllerForTest with NonBusinessTaxRequest with JohnDensmoreTestFixture {
      val result =  method(controllerUnderTest)(request)
      status(result) shouldBe 303
      header("Location", result) shouldBe Some("/")
    }
  }
}

class OtherServicesControllerStandardBehaviourSpec extends OtherServicesControllerBehaviours {

  "Calling otherServices" should {
    behave like aBusinessUserSessionValidatingMethod(controllerUnderTest => controllerUnderTest.otherServices)
  }
}

