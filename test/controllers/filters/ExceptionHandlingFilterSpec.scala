package controllers.filters

import java.util.concurrent.TimeUnit.SECONDS

import akka.util.Timeout
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.play.test.WithFakeApplication

import scala.concurrent.Future

class ExceptionHandlingFilterSpec extends WordSpec with ShouldMatchers with MockitoSugar with WithFakeApplication with ScalaFutures {

  implicit val timeout = Timeout(5, SECONDS)

  "ExceptionHandlingFilter" should {

    "redirect to the returnUrl if there is an exception handling the request" in {
      val returnUrl = "Wa6yuBSzGvUaibkXblJ8aQ%3D%3D"
      implicit val queryStringBindable = model.Encrypted.encryptedStringToDecryptedString

      val fakeRequest = FakeRequest("GET", s"testUrl?returnUrl=$returnUrl")

      val filterResult = ExceptionHandlingFilter(_ => Future.failed(new RuntimeException))(fakeRequest)

      Helpers.redirectLocation(filterResult) shouldBe Some("foo&value")
    }

    "return 500 if there is an exception but no returnUrl in the request" in {
      val fakeRequest = FakeRequest("GET", s"testUrl")
      implicit val queryStringBindable = model.Encrypted.encryptedStringToDecryptedString
      val actionException = new RuntimeException

      val filterResult = ExceptionHandlingFilter(_ => Future.failed(actionException))(fakeRequest)

      filterResult.failed.futureValue shouldBe actionException
    }
  }
}