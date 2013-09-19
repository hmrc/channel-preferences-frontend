package controllers.paye

import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.common.microservice.MockMicroServicesForTests
import play.api.test.{FakeRequest, FakeApplication, WithApplication}
import play.api.test.Helpers._

class CarBenefitHomeControllerSpec extends PayeBaseSpec with MockitoSugar {

  private lazy val controller = new CarBenefitHomeController with MockMicroServicesForTests

  "calling carBenefitHome" should {

    "show car details for user with a company car" in new WithApplication(FakeApplication()) {
      val result = controller.carBenefitHomeAction(johnDensmore, FakeRequest())

      status(result) should be(200)
    //todo finish
    }

    "show Add Car link for a user without a company car" in new WithApplication(FakeApplication()) {
      pending
    }

  }

}
