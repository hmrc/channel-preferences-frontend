package config

import test.BaseSpec
import play.api.mvc._
import org.scalatest.mock.MockitoSugar
import controllers.CookieEncryption
import play.api.test.{FakeApplication, WithApplication, Helpers, FakeRequest}
import scala.Some
import org.joda.time.DateTime

class LastRequestTimestampFilterSpec extends BaseSpec with MockitoSugar with CookieEncryption {

  private val previousRequestTime = new DateTime(2013, 8, 1, 11, 10)
  private val timeToExpiryOfPreviousCookie = 840

  private val currentRequestTime = new DateTime(2013, 8, 1, 11, 11)
  private val timeToExpiryOfNewCookie = 900

  private val filter = new LastRequestTimestampFilter {
    override val now = currentRequestTime
  }

  "Applying the LastRequestTimestampFilter to a request" should {

    "set the last request time cookie if there is no existing cookie" in new WithApplication(FakeApplication()){
      val result = filter(nextFunction)(FakeRequest())
      val newCookie = Helpers.cookies(result)(LastRequestTimestampFilter.lastRequestTimestampCookieName)
      newCookie.maxAge mustBe Some(timeToExpiryOfNewCookie)
      decrypt(newCookie.value).toLong mustBe currentRequestTime.getMillis
    }

    "update the last request time cookie if there is an existing last request time cookie" in {
      val oldCookie = Cookie(LastRequestTimestampFilter.lastRequestTimestampCookieName, encrypt(previousRequestTime.getMillis.toString), Some(timeToExpiryOfPreviousCookie))
      val result = filter(nextFunction)(FakeRequest().withCookies(oldCookie))
      val newCookie = Helpers.cookies(result)(LastRequestTimestampFilter.lastRequestTimestampCookieName)
      newCookie.maxAge mustBe Some(timeToExpiryOfNewCookie)
      decrypt(newCookie.value).toLong mustBe currentRequestTime.getMillis
    }
  }

  private val nextFunction = (requestHeader: RequestHeader) => {
    Results.Ok("FUNCTION CALLED")
  }
}


