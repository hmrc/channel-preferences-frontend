package controllers.bt

import uk.gov.hmrc.common.BaseSpec
import play.api.mvc.{Result, AnyContent, Action}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import scala.Some
import org.joda.time.Duration
import controllers.bt.spechelpers.{NonBusinessUserVatExpectations, GeoffFisherVatExpectations, WithVatApplication}

trait VatControllerBehaviours extends BaseSpec {

  def aVatBusinessUserSessionValidatingMethod(method: VatController => Action[AnyContent]) = {

    "redirect if there is no session" in new WithVatApplication {
      val result: Result = method(vatController)(FakeRequest())
      status(result) shouldBe 303
    }

    "redirect if there is no logged in user" in new WithVatApplication {
      val result: Result = method(vatController)(request)
      status(result) shouldBe 303
    }

    "redirect if the session has timed out" in new WithVatApplication with GeoffFisherVatExpectations {
      override val lastRequestTimestamp = currentTime.minus(Duration.standardMinutes(20))
      val result: Result = method(vatController)(request)
      status(result) shouldBe 303
    }

    "redirect if the user is not a business tax user" in new WithVatApplication with NonBusinessUserVatExpectations {
      val result: Result = method(vatController)(request)
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
