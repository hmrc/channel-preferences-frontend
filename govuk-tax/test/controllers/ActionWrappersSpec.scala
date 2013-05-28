package controllers

import play.api.mvc._
import controllers.service.{ SamlFormData, SamlForm }
import org.scalatest.mock.MockitoSugar
import test.BaseSpec
import org.scalatest.matchers.ShouldMatchers
import play.api.test.{ WithApplication, FakeApplication, FakeRequest }
import play.api.test.Helpers._
import org.mockito.Mockito._
import scala.concurrent.Future
import java.util.UUID

class ActionWrappersSpec extends BaseSpec with ShouldMatchers with MockitoSugar {

  import scala.concurrent.ExecutionContext.Implicits._

  private val mockSamlForm = mock[SamlForm]

  object SomeController extends Controller with ActionWrappers {

    def home = AuthenticatedAction(samlForm = mockSamlForm, block = { implicit request =>
      Async {
        Future(Ok(request.uuid.toString))
      }
    })
  }

  "Calling home without a session cookie" should {

    "return the saml form" in new WithApplication(
      FakeApplication(additionalConfiguration = Map(
        "application.secret" -> "yNhI04vHs9<_HWbC`]20u`37=NGLGYY5:0Tg5?y`W<NoJnXWqmjcgZBec@rOxb^G")
      )
    ) {
      val idaUrl = "http://www.ida.com"
      val samlRequest = "somerandombase64encodedstring"
      val samlFormData = SamlFormData(idaUrl, samlRequest)
      when(mockSamlForm.get).thenReturn(Future(samlFormData))

      val asyncResult = SomeController.home(FakeRequest()).asInstanceOf[AsyncResult]
      val result = await(asyncResult.result, 1)
      status(result) should equal(401)
      contentAsString(result) should include("value=\"" + samlRequest + "\"")
      contentAsString(result) should include("action=\"" + idaUrl + "\"")
    }
  }

  "Calling home with a valid session cookie" should {

    "invoke the handler with the UUID from the cookie available in the request" in new WithApplication(
      FakeApplication(additionalConfiguration = Map(
        "application.secret" -> "yNhI04vHs9<_HWbC`]20u`37=NGLGYY5:0Tg5?y`W<NoJnXWqmjcgZBec@rOxb^G")
      )
    ) {
      val id = UUID.randomUUID().toString
      val asyncResult = SomeController.home(FakeRequest().withSession(("id", id))).asInstanceOf[AsyncResult]
      val result = await(asyncResult.result, 1)
      status(result) should equal(200)
      contentAsString(result) should equal(id)
    }
  }

  "Calling home with an invalid session cookie" should {

    "return the saml form" in new WithApplication(
      FakeApplication(additionalConfiguration = Map(
        "application.secret" -> "yNhI04vHs9<_HWbC`]20u`37=NGLGYY5:0Tg5?y`W<NoJnXWqmjcgZBec@rOxb^G")
      )
    ) {
      val idaUrl = "http://www.ida.com"
      val samlRequest = "somerandombase64encodedstring"
      val samlFormData = SamlFormData(idaUrl, samlRequest)
      when(mockSamlForm.get).thenReturn(Future(samlFormData))

      val asyncResult = SomeController.home(FakeRequest().withSession(("id", "34895938475"))).asInstanceOf[AsyncResult]
      val result = await(asyncResult.result, 1)
      status(result) should equal(401)
      contentAsString(result) should include("value=\"" + samlRequest + "\"")
      contentAsString(result) should include("action=\"" + idaUrl + "\"")
    }
  }
}
