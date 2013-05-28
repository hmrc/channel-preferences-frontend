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

  private val uuid = UUID.randomUUID()

  private val mockTaxUser = mock[TaxUser]

  object TestController extends TestController(uuid)

  class TestController(val uuid: UUID) extends Controller with ActionWrappers {

    def person = FakeAuthenticatingAction(uuid, handler = {
      WithPersonData(mockTaxUser) { implicit request: PersonRequest[AnyContent] =>
        Async {
          Future(Ok(request.person.uri.toString))
        }
      }
    })
  }

  "With Person Data" should {
    "call the wrapped code if a personal taxpayer is returned from the Auth service" in new WithApplication(FakeApplication()) {
      val pid = uuid.toString
      val personUri = URI.create("/person/pid/" + pid)
      val taxUserView = TaxUserView(URI.create("/user/pid/" + pid), Option(personUri))
      when(mockTaxUser.get(pid)).thenReturn(Future(taxUserView))

      implicit val request = PersonRequest(Person(personUri), AuthenticatedRequest(uuid, FakeRequest()))

      val asyncResult = TestController.person(request).asInstanceOf[AsyncResult]
      val result = await(asyncResult.result, 1)
      status(result) should equal(200)
      contentAsString(result) should equal(personUri.toString)
    }

    "return unauthorized if a company taxpayer is returned from the Auth service" in {
      val testUuid = UUID.randomUUID()
      val pid = testUuid.toString
      val taxPayerUriUri = URI.create("/person/pid/" + pid)
      val taxUserView = TaxUserView(URI.create("/user/pid/" + pid), company = Option(URI.create("/company/cid/" + pid)))
      when(mockTaxUser.get(pid)).thenReturn(Future(taxUserView))

      implicit val request = PersonRequest(Person(taxPayerUriUri), AuthenticatedRequest(testUuid, FakeRequest()))

      val asyncResult = new TestController(testUuid).person(request).asInstanceOf[AsyncResult]
      val result = await(asyncResult.result, 1)
      status(result) should equal(401)
    }
  }
}
