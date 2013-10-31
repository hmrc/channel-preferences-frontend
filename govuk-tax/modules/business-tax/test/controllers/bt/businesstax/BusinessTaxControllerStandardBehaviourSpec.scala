package controllers.bt.businesstax

import uk.gov.hmrc.common.BaseSpec
import play.api.mvc.{SimpleResult, Request}
import play.api.test.{WithApplication, FakeApplication}
import play.api.test.Helpers._
import controllers.bt.testframework.request.{NonBusinessTaxRequest, BusinessTaxRequest, EmptySessionRequest, NoSessionRequest}
import controllers.bt.testframework.fixtures.{JohnDensmoreTestFixture, GeoffFisherTestFixture}
import controllers.bt.BusinessTaxController
import controllers.bt.accountsummary.AccountSummariesFactory
import uk.gov.hmrc.common.microservice.domain.User


class BusinessTaxControllerStandardBehaviourSpec extends BaseSpec {

  val mockAccountSummariesFactory = mock[AccountSummariesFactory]
  val controllerUnderTest = new BusinessTaxController(mockAccountSummariesFactory) {
    override private[bt] def businessTaxHomepage(implicit user: User, request: Request[AnyRef]): SimpleResult = Ok
  }

  "Calling home" should {
    "redirect if there is no session" in new WithApplication(FakeApplication()) with NoSessionRequest {
      val result = controllerUnderTest.home(request)
      status(result) shouldBe 303
    }

    "redirect if there is no logged in user" in new WithApplication(FakeApplication()) with EmptySessionRequest {

      val result =  controllerUnderTest.home(request)
      status(result) shouldBe 303
    }

    "redirect if the session has timed out" in new WithApplication(FakeApplication()) with GeoffFisherTestFixture with BusinessTaxRequest {
      override val lastRequestTimestamp = Some(currentTime.minusMinutes(20))
      val result =  controllerUnderTest.home(request)
      status(result) shouldBe 303
    }

    "redirect if the user is not a business tax user" in new WithApplication(FakeApplication()) with NonBusinessTaxRequest with JohnDensmoreTestFixture {
      val result =  controllerUnderTest.home(request)
      status(result) shouldBe 303
      header("Location", result) shouldBe Some("/")
    }
  }
}
