package uk.gov.hmrc.common.filters

import uk.gov.hmrc.common.BaseSpec
import org.scalatest.mock.MockitoSugar
import play.api.mvc._
import scala.concurrent.Future
import play.api.test.{WithApplication, FakeApplication, FakeRequest}
import org.mockito.Mockito._
import org.mockito.Matchers._
import controllers.common.CookieCrypto
import org.mockito.ArgumentCaptor
import org.scalatest.OptionValues
import controllers.common.service.{Encrypter, Decrypter}
import play.api.test.FakeApplication
import play.api.mvc.Cookie
import play.api.mvc.SimpleResult
import org.scalatest.concurrent.ScalaFutures

class CookieCryptoFilterSpec extends BaseSpec with MockitoSugar with CookieCrypto with OptionValues with ScalaFutures {

  trait MockedCrypto extends Encrypter with Decrypter {

    override def decrypt(id: String): String = ???

    override def decrypt(id: Option[String]): Option[String] = ???

    override def encrypt(id: String): String = ???
  }

  val CookieName = "someCookieName"

  trait TestCookieName {
    val cookieName = CookieName
  }

  private trait Setup extends Results {
    val result: SimpleResult = Ok

    lazy val action = {
      val a = mock[(RequestHeader) => Future[SimpleResult]]
      val outgoingResponse = Future.successful(result)
      when(a.apply(any())).thenReturn(outgoingResponse)
      a
    }

    val filter = new CookieCryptoFilter with MockedCrypto with TestCookieName {
      override def decrypt(id: String): String = id match {
        case "encryptedValue" => "decryptedValue"
        case somethingElse => fail(s"Unexpectedly tried to decrypt $somethingElse")
      }
      override def encrypt(id: String): String = id match {
        case "decryptedValue" => "encryptedValue"
        case somethingElse => fail(s"Unexpectedly tried to encrypt $somethingElse")
      }
    }
  }

  "The filter" should {

    "do nothing with no cookie header in the request" in new WithApplication(FakeApplication()) with Setup {
      val incomingRequest = FakeRequest()

      filter(action)(incomingRequest).futureValue should be(result)
      verify(action).apply(incomingRequest)
    }

    "decrypt the cookie" in new WithApplication(FakeApplication()) with Setup {
      val encryptedCookie = Cookie(name = CookieName, value = "encryptedValue")
      val incomingRequest = FakeRequest().withCookies(encryptedCookie)

      filter(action)(incomingRequest)

      val updatedRequest = ArgumentCaptor.forClass(classOf[RequestHeader])
      verify(action).apply(updatedRequest.capture())
      updatedRequest.getValue.cookies.get(CookieName).value should be (encryptedCookie.copy(value = "decryptedValue"))
    }

    "leave empty cookies unchanged" in new WithApplication(FakeApplication()) with Setup {
      val emptyCookie = Cookie(CookieName, "")
      val incomingRequest = FakeRequest().withCookies(emptyCookie)
      filter(action)(incomingRequest).futureValue should be(result)
    }

    "Leave other cookies alone" in new WithApplication(FakeApplication()) with Setup {
      val normalCookie1 = Cookie("AnotherCookie1", "normalValue1")
      val normalCookie2 = Cookie("AnotherCookie2", "normalValue2")
      val incomingRequest = FakeRequest().withCookies(normalCookie1, normalCookie2)
      filter(action)(incomingRequest).futureValue should be(result)

      val updatedRequest = ArgumentCaptor.forClass(classOf[RequestHeader])
      verify(action).apply(updatedRequest.capture())
      updatedRequest.getValue.cookies should contain allOf (normalCookie1, normalCookie2)
    }

    "Leave other cookies alone when ours is present" in new WithApplication(FakeApplication()) with Setup {
      val normalCookie1 = Cookie("AnotherCookie1", "normalValue1")
      val encryptedCookie = Cookie(CookieName, "encryptedValue")
      val normalCookie2 = Cookie("AnotherCookie2", "normalValue2")
      val incomingRequest = FakeRequest().withCookies(normalCookie1, encryptedCookie, normalCookie2)
      filter(action)(incomingRequest).futureValue should be(result)

      val updatedRequest = ArgumentCaptor.forClass(classOf[RequestHeader])
      verify(action).apply(updatedRequest.capture())
      updatedRequest.getValue.cookies should contain allOf (encryptedCookie.copy(value = "decryptedValue"), normalCookie1, normalCookie2)
    }

    "Cope with the decryption failing" in pending

    "discard the session if it contains anything other than the encrypted entry" in pending
    "discard the session if it cannot be decrypted" in pending

    "do nothing with the session result if it is empty" in pending

    "encrypt the cookie value before returning it" in new WithApplication(FakeApplication()) with Setup {
      override val result = Ok.withCookies(Cookie(CookieName, "decryptedValue"))
      val incomingRequest = FakeRequest()

      filter(action)(incomingRequest).futureValue should be(result.withCookies(Cookie(CookieName, "encryptedValue")))
    }

    "ignore the cookie header in the response unchanged if it does not contain the cookie we are looking for" in pending

    "cope with not having a cookie header in the response" in pending
  }
}
