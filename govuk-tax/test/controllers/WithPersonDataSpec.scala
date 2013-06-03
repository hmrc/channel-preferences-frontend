package controllers

import test.BaseSpec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.mock.MockitoSugar
import play.api.test.{ WithApplication, FakeRequest }
import scala.concurrent.Future
import java.net.URI
import play.api.mvc._
import play.api.test.Helpers._
import controllers.service._
import controllers.service.AuthorityData
import controllers.service.PayeData
import play.api.mvc.AsyncResult
import play.api.test.FakeApplication
import controllers.service.BusinessData
import scala.Some
import controllers.service.PersonalData
import org.mockito.Mockito.when

class WithPersonDataSpec extends BaseSpec with ShouldMatchers with MockitoSugar {

  import scala.concurrent.ExecutionContext.Implicits._

  private val userId = URI.create("/auth/oid/132097423895")

  private val payeUri = URI.create("/person/pid/KIHEGKJHDSGKJSDF")

  private val authorityData = AuthorityData(userId.toString, Some(PersonalData(Some(payeUri), None)), None)

  private val mockPersonalTax = mock[PersonalTax]

  object TestController extends TestController(authorityData)

  class TestController(authorityData: AuthorityData) extends Controller with ActionWrappers {

    def person = FakeAuthenticatingAction(authorityData, handler = {
      WithPersonalData(personalTax = mockPersonalTax, handler = { implicit request: PersonalRequest[AnyContent] =>
        Async {
          Future(Ok(request.paye.get.firstName))
        }
      })
    })
  }

  "With Personal Data" should {
    "call the wrapped code if the authority data contains personal data" in new WithApplication(FakeApplication()) {
      val firstName = "John"
      val payeData = PayeData(firstName)
      when(mockPersonalTax.payeData(payeUri.toString)).thenReturn(Future(payeData))

      implicit val request = AuthenticatedRequest(authorityData, FakeRequest())

      val asyncResult = TestController.person(request).asInstanceOf[AsyncResult]
      val result = await(asyncResult.result, 1)
      status(result) should equal(200)
      contentAsString(result) should equal(firstName)
    }

    "return unauthorized if the authority data does not contain personal data" in {
      val authorityData = AuthorityData(userId.toString, None, Some(BusinessData(Some(URI.create("/paye/id/234234")), None, None)))

      implicit val request = AuthenticatedRequest(authorityData, FakeRequest())

      val asyncResult = new TestController(authorityData).person(request).asInstanceOf[AsyncResult]
      val result = await(asyncResult.result, 1)
      status(result) should equal(401)
    }
  }
}
