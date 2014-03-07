package controllers.bt.businesstax

import uk.gov.hmrc.common.BaseSpec
import play.api.test.WithApplication
import play.api.test.Helpers._
import controllers.bt.testframework.request.{NonBusinessTaxRequest, BusinessTaxRequest, EmptySessionRequest, NoSessionRequest}
import controllers.bt.testframework.fixtures.{JohnDensmoreTestFixture, GeoffFisherTestFixture}
import controllers.bt.BusinessTaxController
import controllers.bt.accountsummary.AccountSummariesFactory
import uk.gov.hmrc.common.microservice.preferences.PreferencesConnector
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import uk.gov.hmrc.common.microservice.audit.AuditConnector
import play.api.test.FakeApplication
import scala.Some


class BusinessTaxControllerStandardBehaviourSpec extends BaseSpec {

  val mockAccountSummariesFactory = mock[AccountSummariesFactory]
  val mockPreferencesConnector = mock[PreferencesConnector]
  val mockAuthConnector = mock[AuthConnector]
  val mockAuditConnector = mock[AuditConnector]
  val controllerUnderTest = new BusinessTaxController(mockAccountSummariesFactory, mockPreferencesConnector, mockAuditConnector)(mockAuthConnector)

  "Calling home" should {
    "redirect if there is no session" in new WithApplication(FakeApplication()) with NoSessionRequest {
      val result = controllerUnderTest.home(request)
      status(result) shouldBe 303
    }

    "redirect if there is no logged in user" in new WithApplication(FakeApplication()) with EmptySessionRequest {

      val result = controllerUnderTest.home(request)
      status(result) shouldBe 303
    }

    "redirect if the session has timed out" in new WithApplication(FakeApplication()) with GeoffFisherTestFixture with BusinessTaxRequest {
      override val lastRequestTimestamp = Some(currentTime.minusMinutes(20))
      val result = controllerUnderTest.home(request)
      status(result) shouldBe 303
    }

    "redirect if the user is not a business tax user" in new WithApplication(FakeApplication()) with NonBusinessTaxRequest with JohnDensmoreTestFixture {
      val result = controllerUnderTest.home(request)
      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some(controllers.common.routes.LoginController.businessTaxLogin().url)
    }
  }
}
