package controllers.sa

import uk.gov.hmrc.common.BaseSpec
import org.joda.time.{ DateTimeZone, DateTime }
import play.api.test.{ WithApplication, FakeApplication }
import scala.util.Success

class SecureParameterSpec extends BaseSpec {

  "A secure parameter" should {

    "be unchanged if encrypted and encoded for Urls, url decoded and then decrypted" in new WithApplication(FakeApplication()) {
      val timestamp = DateTime.now(DateTimeZone.UTC)
      val secureParameter = SecureParameter("aValue", timestamp)
      SecureParameter.decrypt(secureParameter.encrypt) shouldBe Success(secureParameter)
    }
  }

  "be able to decode a Base-64 encoded, encrypted string" in new WithApplication(FakeApplication()) {

    val value = "kidYj8rSMumL8z7Z8C+Xt1yTCi49ndsZvK5+fqQXaK70uCY+g7ksy/K8Xs0fJh7t"

    val expected = SecureParameter("aValue", new DateTime(1378218917890L, DateTimeZone.UTC))

    SecureParameter.decrypt(value) shouldBe Success(expected)
  }

  "return a Failure if the value cannot be decoded/decrypted" in new WithApplication(FakeApplication()) {

    val value = "NOT A VALID ENCRYPTED STRING"

    SecureParameter.decrypt(value).isFailure should be(true)
  }

  "be able to encrypt and encode to base 64 format" in new WithApplication(FakeApplication()) {

    val expected = "kidYj8rSMumL8z7Z8C+Xt1yTCi49ndsZvK5+fqQXaK70uCY+g7ksy/K8Xs0fJh7t"

    val value = SecureParameter("aValue", new DateTime(1378218917890L, DateTimeZone.UTC))

    value.encrypt shouldBe expected
  }
}
