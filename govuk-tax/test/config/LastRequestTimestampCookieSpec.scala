package config

import test.BaseSpec
import play.api.mvc._
import org.scalatest.mock.MockitoSugar
import controllers.CookieEncryption
import org.joda.time.{ DateTimeZone, Duration, DateTime }
import play.api.test.{ WithApplication, FakeApplication }
import java.security.GeneralSecurityException

class LastRequestTimestampCookieSpec extends BaseSpec with MockitoSugar with CookieEncryption {

  import LastRequestTimestampCookie._

  private val timeout = Duration.standardMinutes(timeoutMinutes)
  private val oneMillisecond = Duration.millis(1)

  private val currentRequestTime = new DateTime(2013, 8, 1, 11, 11, DateTimeZone.UTC)

  private val invalidRequestTime = new DateTime(2013, 8, 1, 10, 30, DateTimeZone.UTC)

  private val justInvalidRequestTime = currentRequestTime.withDurationAdded(timeout, -1)

  private val justValidRequestTime = justInvalidRequestTime.withDurationAdded(oneMillisecond, 1)

  private val validRequestTime = new DateTime(2013, 8, 1, 11, 10, DateTimeZone.UTC)

  private def validHttpCookie = Cookie(cookieName, encrypt(validRequestTime.getMillis.toString), Some(500))

  private val now = () => currentRequestTime

  "The isValid method" should {

    "return true if the request is in the same millisecond as the previous request" in {
      LastRequestTimestampCookie(currentRequestTime).isValid(now) must be(true)
    }

    "return true if we are within the valid period" in {
      LastRequestTimestampCookie(validRequestTime).isValid(now) must be(true)
    }

    "return true if we are just within the valid period" in {
      LastRequestTimestampCookie(justValidRequestTime).isValid(now) must be(true)
    }

    "return true if we are just without the valid period" in {
      LastRequestTimestampCookie(justInvalidRequestTime).isValid(now) must be(false)
    }

    "return true if we are without the valid period" in {
      LastRequestTimestampCookie(invalidRequestTime).isValid(now) must be(false)
    }
  }

  "Constructing a LastRequestTimestampCookie from a cookie" should {
    "throw an exception if the wrong cookie is supplied" in new WithApplication(FakeApplication()) {
      intercept[IllegalArgumentException] {
        LastRequestTimestampCookie(validHttpCookie.copy(name = "wrongName"))
      }
    }

    "create a valid LastRequestTimestampCookie if the cookie supplied is correct" in new WithApplication(FakeApplication()) {
      LastRequestTimestampCookie(validHttpCookie) mustBe Some(LastRequestTimestampCookie(validRequestTime))
    }

    "throw an exception if the timestamp is not encrypted" in new WithApplication(FakeApplication()) {
      intercept[GeneralSecurityException] {
        LastRequestTimestampCookie(validHttpCookie.copy(value = validRequestTime.getMillis.toString))
      }
    }

    "successfully create a LastRequestTimestampCookie if the timestamp has expired" in new WithApplication(FakeApplication()) {
      val expiredCookie = LastRequestTimestampCookie(validHttpCookie.copy(value = encrypt(invalidRequestTime.getMillis.toString)))
      expiredCookie mustBe Some(LastRequestTimestampCookie(invalidRequestTime))
    }
  }
}

