package uk.gov.hmrc.common.filters

import uk.gov.hmrc.common.BaseSpec
import play.api.mvc.{AnyContentAsEmpty, SimpleResult, RequestHeader}
import play.api.test.{FakeHeaders, FakeRequest}
import scala.concurrent.Future
import org.scalatest.mock.MockitoSugar

class CSRFExceptionsFilterSpec extends BaseSpec with MockitoSugar {

  def action = mock[(RequestHeader) => Future[SimpleResult]]

  "CSRF exceptions filter" should {

    "do nothing if POST request and not ida/login" in {
      val rh = FakeRequest("POST", "/something", FakeHeaders(), AnyContentAsEmpty)

      val requestHeader = CSRFExceptionsFilter.filteredHeaders(rh)

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

  }

}
