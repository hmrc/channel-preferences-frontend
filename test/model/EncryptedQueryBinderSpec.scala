/*
 * Copyright 2019 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package model

import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import play.api.mvc.QueryStringBindable
import uk.gov.hmrc.crypto._
import uk.gov.hmrc.emailaddress.EmailAddress
import org.scalatestplus.play.PlaySpec

class EncryptedQueryBinderSpec extends PlaySpec with MockitoSugar {

  var decryptedEmail: Option[String] = None

  "Binding a Encrypted[Email]" should {
    "Pass through any failure from the string binder" in new TestCase {
      when(stringBinder.bind(any(), any())).thenReturn(Some(Left("an error")))
      binder.bind("exampleKey", Map.empty) must be(Some(Left("an error")))
    }
    "Pass through a None from the string binder" in new TestCase {
      when(stringBinder.bind(any(), any())).thenReturn(None)
      binder.bind("exampleKey", Map.empty) must be(None)
    }
    "Process a validly encrypted valid email" in new TestCase {
      when(stringBinder.bind(any(), any())).thenReturn(Some(Right(encryptedData)))
      decryptedEmail = Some("test@test.com")
      binder.bind("exampleKey", Map.empty) must be(Some(Right(Encrypted(EmailAddress("test@test.com")))))
    }
    "Give an error for an invalid email" in new TestCase {
      when(stringBinder.bind(any(), any())).thenReturn(Some(Right(encryptedData)))
      decryptedEmail = Some("asdfasdf")
      binder.bind("exampleKey", Map.empty) must be(Some(Left("exampleKey is not valid")))
    }
    "Give an error if decryption throws an exception" in new TestCase {
      when(stringBinder.bind(any(), any())).thenReturn(Some(Right(encryptedData)))
      decryptedEmail = None
      binder.bind("exampleKey", Map.empty) must be(Some(Left("Could not decrypt value for exampleKey")))
    }
  }

  trait TestCase {
    val stringBinder = mock[QueryStringBindable[String]]
    val crypto = new Encrypter with Decrypter {
      override def decrypt(reversiblyEncrypted: Crypted): PlainText =
        decryptedEmail.map(PlainText).getOrElse(throw new RuntimeException())

      override def encrypt(plain: PlainContent): Crypted = ???

      override def decryptAsBytes(reversiblyEncrypted: Crypted): PlainBytes = ???
    }
    val binder = new EncryptedQueryBinder[EmailAddress](crypto, EmailAddress.apply, _.value)(stringBinder)
    val encryptedData: String = "encrypted Data"
  }
}
