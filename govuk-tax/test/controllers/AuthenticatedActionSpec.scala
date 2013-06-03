package controllers

import play.api.mvc._
import controllers.service._
import org.scalatest.mock.MockitoSugar
import test.BaseSpec
import org.scalatest.matchers.ShouldMatchers
import play.api.test.{ WithApplication, FakeRequest }
import play.api.test.Helpers._
import org.mockito.Mockito._
import scala.concurrent.Future
import java.net.URI
import controllers.service.SamlFormData
import play.api.mvc.AsyncResult
import play.api.test.FakeApplication
import scala.Some

class AuthenticatedActionSpec extends BaseSpec with ShouldMatchers with MockitoSugar {

  import scala.concurrent.ExecutionContext.Implicits._

  private val mockSamlForm = mock[SamlForm]

  private val mockAuthority = mock[Authority]

  object SomeController extends Controller with ActionWrappers {

    def home = AuthenticatedAction(samlForm = mockSamlForm, authority = mockAuthority, handler = { implicit request =>
      Async {
        Future(Ok(request.authority.personal.get.paye.get.toString))
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

    "invoke the handler with the authority data returned from the auth service" in new WithApplication(
      FakeApplication(additionalConfiguration = Map(
        "application.secret" -> "yNhI04vHs9<_HWbC`]20u`37=NGLGYY5:0Tg5?y`W<NoJnXWqmjcgZBec@rOxb^G")
      )
    ) {
      val id = "/auth/oid/9345092835029385"
      val payeUri = URI.create("/user/uid/KUHFKUGEFKUEGH")

      val authorityData = AuthorityData(id, Some(PersonalData(Some(payeUri), None)), None)
      when(mockAuthority.get(id)).thenReturn(Future(authorityData))

      val asyncResult = SomeController.home(FakeRequest().withSession(("id", id))).asInstanceOf[AsyncResult]
      val result = await(asyncResult.result, 1)
      status(result) should equal(200)
      contentAsString(result) should equal(payeUri.toString)
    }
  }
}
