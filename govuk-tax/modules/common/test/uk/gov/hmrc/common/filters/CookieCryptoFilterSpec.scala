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

class CookieCryptoFilterSpec extends BaseSpec with MockitoSugar with CookieCrypto with OptionValues {

  trait MockedCrypto extends Encrypter with Decrypter {

    override def decrypt(id: String): String = ???

    override def decrypt(id: Option[String]): Option[String] = ???

    override def encrypt(id: String): String = ???
  }

  val CookieName = "someCookieName"

  trait TestCookieName {
    val cookieName = CookieName
  }

  private trait Setup {
    val action = mock[(RequestHeader) => Future[SimpleResult]]
    val outgoingResponse = Future.successful(mock[SimpleResult])
  }

  "The filter" should {

    "do nothing with the cookie if it is missing" in new WithApplication(FakeApplication()) {
      new Setup {
        val filter = new CookieCryptoFilter with MockedCrypto with TestCookieName
        val incomingRequest = FakeRequest()

        when(action.apply(any())).thenReturn(outgoingResponse)
        filter(action)(incomingRequest) should be(outgoingResponse)
        verify(action).apply(incomingRequest)
      }
    }

    "decrypt the cookie" in new WithApplication(FakeApplication()) {
      new Setup {
        val filter = new CookieCryptoFilter with MockedCrypto with TestCookieName {
          override def decrypt(id: String): String = id match {
            case "encryptedValue" => "decryptedValue"
            case somethingElse => fail(s"Unexpectedly tried to decrypt $somethingElse")
          }
        }
        val encryptedCookie = Cookie(name = CookieName, value = "encryptedValue")
        val incomingRequest = FakeRequest().withCookies(encryptedCookie)

        filter(action)(incomingRequest)

        val updatedRequest = ArgumentCaptor.forClass(classOf[RequestHeader])
        verify(action).apply(updatedRequest.capture())
        updatedRequest.getValue.cookies.get(CookieName).value should be (encryptedCookie.copy(value = "decryptedValue"))
      }
    }

    "leave empty cookies unchanged" in new WithApplication(FakeApplication()) {
      new Setup {
        val filter = new CookieCryptoFilter with MockedCrypto with TestCookieName
        val emptyCookie = Cookie(CookieName, "")
        val incomingRequest = FakeRequest().withCookies(emptyCookie)
        when(action.apply(any())).thenReturn(outgoingResponse)
        filter(action)(incomingRequest) should be(outgoingResponse)
      }
    }

    "Leave other cookies alone" in new WithApplication(FakeApplication()) {
      new Setup {
        val filter = new CookieCryptoFilter with MockedCrypto with TestCookieName {
          override def decrypt(id: String): String = id match {
            case "encryptedValue" => "decryptedValue"
            case somethingElse => fail(s"Unexpectedly tried to decrypt $somethingElse")
          }
        }
        val normalCookie1 = Cookie("AnotherCookie1", "normalValue1")
        val encryptedCookie = Cookie(CookieName, "encryptedValue")
        val normalCookie2 = Cookie("AnotherCookie2", "normalValue2")
        val incomingRequest = FakeRequest().withCookies(normalCookie1, encryptedCookie, normalCookie2)
        when(action.apply(any())).thenReturn(outgoingResponse)
        filter(action)(incomingRequest) should be(outgoingResponse)
        
        val updatedRequest = ArgumentCaptor.forClass(classOf[RequestHeader])
        verify(action).apply(updatedRequest.capture())
        updatedRequest.getValue.cookies should contain allOf (encryptedCookie.copy(value = "decryptedValue"), normalCookie1, normalCookie2)
      }
    }
    
    "Cope with the decryption failing" in pending

    "discard the session if it contains anything other than the encrypted entry" in pending
    "discard the session if it cannot be decrypted" in pending

    "do nothing with the session result if it is empty" in pending

    "replace the values in the resulting session with a single encrypted value" in pending
  }
}
