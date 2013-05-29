package controllers

import test.BaseSpec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.mock.MockitoSugar
import controllers.service.TaxUser
import play.api.test.{ WithApplication, FakeRequest }
import org.mockito.Mockito._
import java.util.UUID
import scala.concurrent.Future
import java.net.URI
import play.api.mvc._
import play.api.test.Helpers._
import play.api.mvc.AsyncResult
import play.api.test.FakeApplication
import controllers.service.TaxUserView

class WithPersonDataSpec extends BaseSpec with ShouldMatchers with MockitoSugar {

  import scala.concurrent.ExecutionContext.Implicits._

  private val userId = URI.create("/auth/oid/132097423895")

  private val personUri = URI.create("/person/pid/KIHEGKJHDSGKJSDF")

  private val taxUserView = TaxUserView(userId, Some(personUri))

  object TestController extends TestController(taxUserView)

  class TestController(taxUserView: TaxUserView) extends Controller with ActionWrappers {

    def person = FakeAuthenticatingAction(taxUserView, handler = {
      WithPersonalData { implicit request: PersonRequest[AnyContent] =>
        Async {
          Future(Ok(request.person.uri.toString))
        }
      }
    })
  }

  "With Personal Data" should {
    "call the wrapped code if the tax user view contains a person uri" in new WithApplication(FakeApplication()) {

      implicit val request = PersonRequest(Person(personUri), AuthenticatedRequest(taxUserView, FakeRequest()))

      val asyncResult = TestController.person(request).asInstanceOf[AsyncResult]
      val result = await(asyncResult.result, 1)
      status(result) should equal(200)
      contentAsString(result) should equal(personUri.toString)
    }

    "return unauthorized if the tax user view does not contain a person uri" in {
      val taxUserView = TaxUserView(userId, company = Option(URI.create("/company/cid/897324598273598")))

      implicit val request = AuthenticatedRequest(taxUserView, FakeRequest())

      val asyncResult = new TestController(taxUserView).person(request).asInstanceOf[AsyncResult]
      val result = await(asyncResult.result, 1)
      status(result) should equal(401)
    }
  }
}
