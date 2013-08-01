package config

import test.BaseSpec
import play.api.mvc.{Cookie, Cookies, Result, RequestHeader}
import org.scalatest.mock.MockitoSugar
import play.api.test.FakeRequest
import org.mockito.Mockito._
import controllers.CookieEncryption
import org.joda.time.DateTime
import TimeoutFilter.lastRequestTimestampCookieName
import TimeoutFilter.timeoutMinutes

class TimeoutFilterSpec extends BaseSpec with MockitoSugar with CookieEncryption {

  "Applying the TimeoutFilter to a request" should {

    "set the last request time cookie if there are no existing cookies" in {
      val mockRequestHeader = makeRequest(None)
      TimeoutFilter(nextFunction)(mockRequestHeader)
    }

    "update the last request time cookie if there is an existing last request time cookie" in {
      pending
    }

    "delete the session cookie if there is one, but there is no last request time cookie (and set the last request time)" in {
      pending
    }

    "delete the session cookie if there is one, but the last request time is older than 15 minutes (and update the last request time)" in {
      pending
    }

    "allow the session cookie through if the last request time is less than 15 minutes ago (and update the last request time)" in {
      pending
    }
  }

  private def makeRequest(lastRequestTimestamp: Option[Long]): RequestHeader = {
    val mockRequestHeader = mock[RequestHeader]
    val mockCookies = mock[Cookies]

    when(mockRequestHeader.cookies).thenReturn(mockCookies)

    for (timestamp <- lastRequestTimestamp) {
      when(mockCookies.get(lastRequestTimestampCookieName)).thenReturn(Some(cookie(timestamp)))
    }

    mockRequestHeader
  }

  private def cookie(timestamp: Long) = {
    val encodedTimestamp = encrypt(timestamp.toString)
    Cookie(lastRequestTimestampCookieName, encodedTimestamp, Some(timeoutMinutes * 60))
  }

  private val nextFunction = (requestHeader: RequestHeader) => {
    mock[Result]
  }
}


