package controllers.bt.businesstax

import uk.gov.hmrc.common.BaseSpec
import play.api.mvc.{Result, AnyContent, Action}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import controllers.bt.testframework.request.{NonBusinessTaxRequest, BusinessTaxRequest, EmptySessionRequest, NoSessionRequest}
import controllers.bt.testframework.fixtures.{JohnDensmoreTestFixture, GeoffFisherTestFixture}
import controllers.bt.BusinessTaxController

trait BusinessTaxControllerBehaviours extends BaseSpec {

  def aBusinessUserSessionValidatingMethod(method: BusinessTaxController => Action[AnyContent]) {

    "redirect if there is no session" in new BusinessTaxControllerForTest with NoSessionRequest {
      val result: Result = method(controllerUnderTest)(FakeRequest())
      status(result) shouldBe 303
    }

    "redirect if there is no logged in user" in new BusinessTaxControllerForTest with EmptySessionRequest {
      val result: Result =  method(controllerUnderTest)(request)
      status(result) shouldBe 303
    }

    "redirect if the session has timed out" in new BusinessTaxControllerForTest with GeoffFisherTestFixture with BusinessTaxRequest {
      override val lastRequestTimestamp = Some(currentTime.minusMinutes(20))
      val result: Result =  method(controllerUnderTest)(request)
      status(result) shouldBe 303
    }

    "redirect if the user is not a business tax user" in new BusinessTaxControllerForTest with NonBusinessTaxRequest with JohnDensmoreTestFixture {
      val result: Result =  method(controllerUnderTest)(request)
      status(result) shouldBe 303
      header("Location", result) shouldBe Some("/")
    }
  }
}

class BusinessTaxControllerStandardBehaviourSpec extends BusinessTaxControllerBehaviours {

  "Calling makeAPaymentLanding" should {
    behave like aBusinessUserSessionValidatingMethod(controllerUnderTest => controllerUnderTest.makeAPaymentLanding)
  }

  "Calling home" should {
    behave like aBusinessUserSessionValidatingMethod(controllerUnderTest => controllerUnderTest.home)
  }
}

