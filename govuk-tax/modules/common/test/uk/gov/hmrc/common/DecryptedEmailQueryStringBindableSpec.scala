package uk.gov.hmrc.common

import play.api.mvc.QueryStringBindable
import org.mockito.Mockito._
import org.mockito.Matchers._
import uk.gov.hmrc.crypto.{Encrypter, Decrypter}
import uk.gov.hmrc.domain.Email
import uk.gov.hmrc.common.crypto.Decrypted


class DecryptedEmailQueryStringBindableSpec extends BaseSpec {
  "Binding a Decrypted[Email]" should {
    "Pass through any failure from the string binder" in new TestCase {
      when(stringBinder.bind(any(), any())).thenReturn(Some(Left("an error")))
      binder.bind("exampleKey", Map.empty) should be (Some(Left("an error")))
    }
    "Pass through a None from the string binder" in new TestCase {
      when(stringBinder.bind(any(), any())).thenReturn(None)
      binder.bind("exampleKey", Map.empty) should be (None)
    }
    "Process a validly encrypted valid email" in new TestCase {
      private val encryptedData: String = "encrypted Data"
      when(stringBinder.bind(any(), any())).thenReturn(Some(Right(encryptedData)))
      when(crypto.decrypt(encryptedData)).thenReturn("test@test.com")
      binder.bind("exampleKey", Map.empty) should be (Some(Right(Decrypted(Email("test@test.com")))))
    }
    "Give an error for an invalid email" in new TestCase {
      private val encryptedData: String = "encrypted Data"
      when(stringBinder.bind(any(), any())).thenReturn(Some(Right(encryptedData)))
      when(crypto.decrypt(encryptedData)).thenReturn("asdfasdf")
      binder.bind("exampleKey", Map.empty) should be (Some(Left("Not a valid email address")))
    }
    "Give an error if decryption throws an exception" in new TestCase {
      private val encryptedData: String = "encrypted Data"
      when(stringBinder.bind(any(), any())).thenReturn(Some(Right(encryptedData)))
      when(crypto.decrypt(encryptedData)).thenThrow(new RuntimeException())
      binder.bind("exampleKey", Map.empty) should be (Some(Left("Could not decrypt value")))
    }
  }

  trait TestCase {
    val stringBinder = mock[QueryStringBindable[String]]
    trait Crypto extends Encrypter with Decrypter // appease the Mockito fairies
    val crypto = mock[Crypto]
    val binder = new DecryptedEmailQueryStringBindable(crypto, stringBinder)
  }
}
