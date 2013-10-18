package controllers.bt

import uk.gov.hmrc.common.BaseSpec
import play.api.mvc.{Result, AnyContent, Action}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import org.joda.time.Duration
import controllers.bt.vat._
import scala.Some

trait VatControllerBehaviours extends BaseSpec {

  def aVatBusinessUserSessionValidatingMethod(method: VatController => Action[AnyContent]) {

    "redirect if there is no session" in new VatControllerForTest with RequestWithoutSession {
      val result: Result = method(vatControllerUnderTest)(FakeRequest())
      status(result) shouldBe 303
    }

    "redirect if there is no logged in user" in new VatControllerForTest with RequestWithEmptySession {
      val result: Result = method(vatControllerUnderTest)(request)
      status(result) shouldBe 303
    }

    "redirect if the session has timed out" in new VatControllerForTest with GeoffFisherTestFixture with BusinessTaxRequest {
      override val lastRequestTimestamp = Some(currentTime.minus(Duration.standardMinutes(20)))
      val result: Result = method(vatControllerUnderTest)(request)
      status(result) shouldBe 303
    }

    "redirect if the user is not a business tax user" in new VatControllerForTest with NonBusinessTaxRequest with JohnDensmoreTestFixture {
      val result: Result = method(vatControllerUnderTest)(request)
      status(result) shouldBe 303
      header("Location", result) shouldBe Some("/")
    }
  }
}

class VatControllerStandardBehaviourSpec extends VatControllerBehaviours {

  "Calling makeAPayment" should {
    behave like aVatBusinessUserSessionValidatingMethod(vatController => vatController.makeAPayment)
  }

}

