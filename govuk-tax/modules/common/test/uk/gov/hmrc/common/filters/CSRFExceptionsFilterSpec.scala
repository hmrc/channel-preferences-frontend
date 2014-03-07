package uk.gov.hmrc.common.filters

import uk.gov.hmrc.common.BaseSpec
import play.api.mvc.AnyContentAsEmpty
import play.api.test.{FakeApplication, WithApplication, FakeRequest, FakeHeaders}
import org.scalatest.mock.MockitoSugar
import controllers.common.{SessionTimeoutWrapper, SessionKeys}
import org.joda.time.{DateTimeZone, DateTime}
import uk.gov.hmrc.utils.DateTimeUtils

class CSRFExceptionsFilterSpec extends BaseSpec with MockitoSugar {

  private val now = () =>  DateTimeUtils.now

  "CSRF exceptions filter" should {

    "do nothing if POST request and not ida/login" in new WithApplication(new FakeApplication()) {
      val validTime = now().minusSeconds(SessionTimeoutWrapper.timeoutSeconds/2).getMillis.toString
      val rh = FakeRequest("POST", "/something", FakeHeaders(), AnyContentAsEmpty).withSession(SessionKeys.lastRequestTimestamp -> validTime)

      val requestHeader = CSRFExceptionsFilter.filteredHeaders(rh, now)

      requestHeader.headers.get("Csrf-Token") shouldBe None
    }

    "do nothing for GET requests" in {
      val rh = FakeRequest("GET", "/ida/login", FakeHeaders(), AnyContentAsEmpty)

      val requestHeader = CSRFExceptionsFilter.filteredHeaders(rh)

      requestHeader.headers.get("Csrf-Token") shouldBe None
    }

    "add Csrf-Token header with value nocheck to bypass validation for ida/login POST request" in {
      val rh = FakeRequest("POST", "/ida/login", FakeHeaders(), AnyContentAsEmpty)

      val requestHeader = CSRFExceptionsFilter.filteredHeaders(rh)

      requestHeader.headers("Csrf-Token") shouldBe "nocheck"
    }

    "add Csrf-Token header with value nocheck to bypass validation for SSO POST request" in {
      val rh = FakeRequest("POST", "/ssoin", FakeHeaders(), AnyContentAsEmpty)

      val requestHeader = CSRFExceptionsFilter.filteredHeaders(rh)

      requestHeader.headers("Csrf-Token") shouldBe "nocheck"
    }

    "add Csrf-Token header with value nocheck to bypass validation if the session has expired" in new WithApplication(FakeApplication()) {
      val invalidTime = new DateTime(2012, 7, 7, 4, 6, 20, DateTimeZone.UTC).minusDays(1).getMillis.toString
      val rh = FakeRequest("POST", "/some/post", FakeHeaders(), AnyContentAsEmpty).withSession(SessionKeys.lastRequestTimestamp -> invalidTime)

      val requestHeader = CSRFExceptionsFilter.filteredHeaders(rh, now)

      requestHeader.headers("Csrf-Token") shouldBe "nocheck"
    }

  }

}
