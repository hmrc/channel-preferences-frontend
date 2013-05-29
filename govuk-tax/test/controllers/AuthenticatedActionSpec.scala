package controllers

import play.api.mvc._
import controllers.service.{ TaxUserView, TaxUser, SamlFormData, SamlForm }
import org.scalatest.mock.MockitoSugar
import test.BaseSpec
import org.scalatest.matchers.ShouldMatchers
import play.api.test.{ WithApplication, FakeApplication, FakeRequest }
import play.api.test.Helpers._
import org.mockito.Mockito._
import scala.concurrent.Future
import java.net.URI

class AuthenticatedActionSpec extends BaseSpec with ShouldMatchers with MockitoSugar {

  import scala.concurrent.ExecutionContext.Implicits._

  private val mockSamlForm = mock[SamlForm]

  private val mockTaxUser = mock[TaxUser]

  object SomeController extends Controller with ActionWrappers {

    def home = AuthenticatedAction(samlForm = mockSamlForm, taxUser = mockTaxUser, handler = { implicit request =>
      Async {
        Future(Ok(request.taxUserView.person.get.toString))
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

    "invoke the handler with the TaxUserView returned from the auth service" in new WithApplication(
      FakeApplication(additionalConfiguration = Map(
        "application.secret" -> "yNhI04vHs9<_HWbC`]20u`37=NGLGYY5:0Tg5?y`W<NoJnXWqmjcgZBec@rOxb^G")
      )
    ) {
      val id = "9345092835029385"
      val personUri = URI.create("/user/uid/KUHFKUGEFKUEGH")

      val taxUserView = TaxUserView(URI.create("/foo"), Some(personUri))
      when(mockTaxUser.get(id)).thenReturn(Future(taxUserView))

      val asyncResult = SomeController.home(FakeRequest().withSession(("id", id))).asInstanceOf[AsyncResult]
      val result = await(asyncResult.result, 1)
      status(result) should equal(200)
      contentAsString(result) should equal(personUri.toString)
    }
  }
}
