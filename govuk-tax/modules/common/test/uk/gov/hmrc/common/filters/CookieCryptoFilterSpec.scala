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
import org.scalatest.{Inspectors, OptionValues}
import controllers.common.service.{Encrypter, Decrypter}
import play.api.test.FakeApplication
import play.api.mvc.Cookie
import play.api.mvc.SimpleResult
import org.scalatest.concurrent.ScalaFutures
import play.api.http.HeaderNames

class CookieCryptoFilterSpec extends BaseSpec with MockitoSugar with CookieCrypto with OptionValues with ScalaFutures with Inspectors {

  trait MockedCrypto extends Encrypter with Decrypter {
    override def decrypt(id: String): String = ???
    override def encrypt(id: String): String = ???
  }

  val CookieName = "someCookieName"

  trait TestCookieName {
    val cookieName = CookieName
  }

  private trait Setup extends Results {
    val result: SimpleResult = Ok

    lazy val action = {
      val mockAction = mock[(RequestHeader) => Future[SimpleResult]]
      val outgoingResponse = Future.successful(result)
      when(mockAction.apply(any())).thenReturn(outgoingResponse)
      mockAction
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

  "During request pre-processing, the filter" should {

    "do nothing with no cookie header in the request" in new WithApplication(FakeApplication()) with Setup {
      val incomingRequest = FakeRequest()

      filter(action)(incomingRequest).futureValue should be(result)
      val updatedRequest = ArgumentCaptor.forClass(classOf[RequestHeader])
      verify(action).apply(updatedRequest.capture())
      val value = updatedRequest.getValue

      value should equal (incomingRequest)
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

    "leave other cookies alone if our cookie is not present" in new WithApplication(FakeApplication()) with Setup {
      val normalCookie1 = Cookie("AnotherCookie1", "normalValue1")
      val normalCookie2 = Cookie("AnotherCookie2", "normalValue2")
      val incomingRequest = FakeRequest().withCookies(normalCookie1, normalCookie2)
      filter(action)(incomingRequest).futureValue should be(result)

      val updatedRequest = ArgumentCaptor.forClass(classOf[RequestHeader])
      verify(action).apply(updatedRequest.capture())
      updatedRequest.getValue.cookies should contain allOf (normalCookie1, normalCookie2)
    }

    "leave other cookies alone when ours is present" in new WithApplication(FakeApplication()) with Setup {
      val normalCookie1 = Cookie("AnotherCookie1", "normalValue1")
      val encryptedCookie = Cookie(CookieName, "encryptedValue")
      val normalCookie2 = Cookie("AnotherCookie2", "normalValue2")
      val incomingRequest = FakeRequest().withCookies(normalCookie1, encryptedCookie, normalCookie2)
      filter(action)(incomingRequest).futureValue should be(result)

      val updatedRequest = ArgumentCaptor.forClass(classOf[RequestHeader])
      verify(action).apply(updatedRequest.capture())
      updatedRequest.getValue.cookies should contain allOf (encryptedCookie.copy(value = "decryptedValue"), normalCookie1, normalCookie2)
    }

   "remove the cookie header if the decryption fails and there are no other cookies" in new WithApplication(FakeApplication()) with Setup {
      val encryptedCookie = Cookie(name = CookieName, value = "invalidEncryptedValue")
      val incomingRequest = FakeRequest().withCookies(encryptedCookie)

      filter(action)(incomingRequest)

      val updatedRequest = ArgumentCaptor.forClass(classOf[RequestHeader])
      verify(action).apply(updatedRequest.capture())

      updatedRequest.getValue.cookies should be (empty)
      updatedRequest.getValue.headers.toMap should not contain key (HeaderNames.COOKIE)
    }

    "remove the cookie (but leave other cookies intact) if with the decryption fails" in new WithApplication(FakeApplication()) with Setup {
      val normalCookie1 = Cookie("AnotherCookie1", "normalValue1")
      val encryptedCookie = Cookie(name = CookieName, value = "invalidEncryptedValue")
      val normalCookie2 = Cookie("AnotherCookie2", "normalValue2")

      val incomingRequest = FakeRequest().withCookies(normalCookie1, encryptedCookie, normalCookie2)

      filter(action)(incomingRequest)

      val updatedRequest = ArgumentCaptor.forClass(classOf[RequestHeader])
      verify(action).apply(updatedRequest.capture())

      updatedRequest.getValue.cookies should contain only (normalCookie1, normalCookie2)
    }
  }

  "During result post-processing, the filter" should {

    "do nothing with the result if there are no cookies" in new WithApplication(FakeApplication()) with Setup {
      filter(action)(FakeRequest()).futureValue should be(result)
    }

    "do nothing with the result if there are cookies, but not our cookie" in new WithApplication(FakeApplication()) with Setup {

      val normalCookie1 = Cookie("AnotherCookie1", "normalValue1")
      val normalCookie2 = Cookie("AnotherCookie2", "normalValue2")

      override val result = Ok.withCookies(normalCookie1, normalCookie2)
      val incomingRequest = FakeRequest()

      filter(action)(incomingRequest).futureValue should be(result)
    }

    "encrypt the cookie value before returning it" in new WithApplication(FakeApplication()) with Setup {
      override val result = Ok.withCookies(Cookie(CookieName, "decryptedValue"))
      val incomingRequest = FakeRequest()

      filter(action)(incomingRequest).futureValue should be(Ok.withCookies(Cookie(CookieName, "encryptedValue")))
    }

    "encrypt the cookie value before returning it, leaving other cookies unchanged" in new WithApplication(FakeApplication()) with Setup {

      val normalCookie1 = Cookie("AnotherCookie1", "normalValue1")
      val ourCookie = Cookie(CookieName, "decryptedValue")
      val normalCookie2 = Cookie("AnotherCookie2", "normalValue2")

      override val result = Ok.withCookies(normalCookie1, ourCookie, normalCookie2)
      val incomingRequest = FakeRequest()

      filter(action)(incomingRequest).futureValue should be(Ok.withCookies(normalCookie1, normalCookie2, Cookie(CookieName, "encryptedValue")))
    }

    "do nothing with the cookie value if it is empty" in new WithApplication(FakeApplication()) with Setup {
      override val result = Ok.withCookies(Cookie(CookieName, ""))
      val incomingRequest = FakeRequest()

      filter(action)(incomingRequest).futureValue should be(Ok.withCookies(Cookie(CookieName, "")))
    }
  }
}
