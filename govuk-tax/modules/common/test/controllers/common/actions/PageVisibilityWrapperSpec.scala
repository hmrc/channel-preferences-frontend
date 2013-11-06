package controllers.common.actions

import play.api.mvc._
import uk.gov.hmrc.common.BaseSpec
import play.api.test.{FakeRequest, WithApplication, FakeApplication}
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.common.microservice.domain.User
import play.api.test.Helpers._

class PageVisibilityWrapperSpec extends BaseSpec with MockitoSugar {

  "PageVisibilityWrapper " should {

    "authorise the action requested if the predicate is true" in new WithApplication(FakeApplication()) {

      val positivePredicate = new PageVisibilityPredicate {
        def isVisible(user: User, request: Request[AnyContent]): Boolean = true
      }
      val userMock = mock[User]

      val result = DummyController.action(positivePredicate, userMock)

      status(result.apply(FakeRequest())) shouldBe 200
    }

    "Non authorise the action requested if the predicate is false" in {

      val negativePredicate = new PageVisibilityPredicate {
        def isVisible(user: User, request: Request[AnyContent]): Boolean = false
      }

      val userMock = mock[User]

      val result = DummyController.action(negativePredicate, userMock)

      status(result.apply(FakeRequest())) shouldBe 404

    }
  }


  object DummyController extends Controller {

    def action(predicate: PageVisibilityPredicate, user: User) = WithPageVisibility(predicate, user) {
      user =>
        Action(Ok)
    }
  }

}
