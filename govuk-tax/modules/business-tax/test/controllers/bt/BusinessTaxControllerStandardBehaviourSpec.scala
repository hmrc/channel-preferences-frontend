package controllers.bt

import uk.gov.hmrc.common.BaseSpec
import play.api.mvc.{Result, AnyContent, Action}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import scala.Some
import org.joda.time.Duration
import controllers.bt.spechelpers.{NonBusinessUserExpectations, GeoffFisherExpectations, WithBusinessTaxApplication}

trait BusinessTaxControllerBehaviours extends BaseSpec {

  def aBusinessUserSessionValidatingMethod(method: BusinessTaxController => Action[AnyContent]) = {

    "redirect if there is no session" in new WithBusinessTaxApplication {
      val result: Result = method(businessTaxController)(FakeRequest())
      status(result) shouldBe 303
    }

    "redirect if there is no logged in user" in new WithBusinessTaxApplication {
      val result: Result =  method(businessTaxController)(request)
      status(result) shouldBe 303
    }

    "redirect if the session has timed out" in new WithBusinessTaxApplication with GeoffFisherExpectations {
      override val lastRequestTimestamp = currentTime.minus(Duration.standardMinutes(20))
      val result: Result =  method(businessTaxController)(request)
      status(result) shouldBe 303
    }

    "redirect if the user is not a business tax user" in new WithBusinessTaxApplication with NonBusinessUserExpectations {
      val result: Result =  method(businessTaxController)(request)
      status(result) shouldBe 303
      header("Location", result) shouldBe Some("/")
    }
  }
}

class BusinessTaxControllerStandardBehaviourSpec extends BusinessTaxControllerBehaviours {

  "Calling makeAPaymentLanding" should {
    behave like aBusinessUserSessionValidatingMethod(businessTaxController => businessTaxController.makeAPaymentLanding)
  }

  "Calling home" should {
    behave like aBusinessUserSessionValidatingMethod(businessTaxController => businessTaxController.home)
  }
}

