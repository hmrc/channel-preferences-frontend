package uk.gov.hmrc.common.filters

import uk.gov.hmrc.common.BaseSpec
import org.scalatest.mock.MockitoSugar
import play.api.mvc._
import scala.concurrent.Future
import play.api.test._
import org.mockito.Mockito._
import org.mockito.Matchers._
import org.mockito.ArgumentCaptor
import org.scalatest.concurrent.ScalaFutures
import play.api.mvc.SimpleResult
import play.api.test.FakeApplication
import play.api.http.HeaderNames

class CacheControlFilterSpec extends BaseSpec with MockitoSugar with ScalaFutures {

  private trait Setup extends Results {

    val expectedCacheControlHeader = HeaderNames.CACHE_CONTROL -> "no-cache,no-store,max-age=0"

    val resultFromAction: SimpleResult = Ok

    lazy val action = {
      val mockAction = mock[(RequestHeader) => Future[SimpleResult]]
      val outgoingResponse = Future.successful(resultFromAction)
      when(mockAction.apply(any())).thenReturn(outgoingResponse)
      mockAction
    }

    def requestPassedToAction = {
      val updatedRequest = ArgumentCaptor.forClass(classOf[RequestHeader])
      verify(action).apply(updatedRequest.capture())
      updatedRequest.getValue
    }
  }

  "During request pre-processing, the filter" should {

    "do nothing, just pass on the request" in new WithApplication(FakeApplication()) with Setup {
      CacheControlFilter(action)(FakeRequest())
      requestPassedToAction should === (FakeRequest())
    }
  }

  "During result post-processing, the filter" should {

    "add a cache-control header if there isn't one" in new WithApplication(FakeApplication()) with Setup {
      CacheControlFilter(action)(FakeRequest()).futureValue should be(Ok.withHeaders(expectedCacheControlHeader))
    }

    "replace any existing cache-control header" in new WithApplication(FakeApplication()) with Setup {
      override val resultFromAction = Ok.withHeaders(HeaderNames.CACHE_CONTROL -> "someOtherValue")
      CacheControlFilter(action)(FakeRequest()).futureValue should be(Ok.withHeaders(expectedCacheControlHeader))
    }

    "leave any other headers alone" in new WithApplication(FakeApplication()) with Setup {
      override val resultFromAction = Ok.withHeaders(
        "header1" -> "value1",
        HeaderNames.CACHE_CONTROL -> "someOtherValue",
        "header2" -> "value2")

      CacheControlFilter(action)(FakeRequest()).futureValue should be(Ok.withHeaders(
        "header1" -> "value1",
        expectedCacheControlHeader,
        "header2" -> "value2"))
    }
  }
}
