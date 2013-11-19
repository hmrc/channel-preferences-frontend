package uk.gov.hmrc.common.microservice.vat

import uk.gov.hmrc.common.BaseSpec
import org.scalatest.mock.MockitoSugar
import play.api.test.WithApplication
import org.mockito.Mockito._
import play.api.test.FakeApplication
import uk.gov.hmrc.microservice.MicroServiceException
import play.api.libs.ws.Response
import uk.gov.hmrc.common.microservice.vat.domain.{VatAccountBalance, VatAccountSummary, VatJsonRoot}
import uk.gov.hmrc.common.microservice.ct.domain.{AccountingPeriod, CalendarEvent}
import org.joda.time.LocalDate

class VatConnectorSpec extends BaseSpec {

  "Requesting the VAT root" should {

    "return the root object for a successful response" in new VatConnectorApplication {

      val vatRoot = VatJsonRoot(Map("some" -> "link"))

      when(mockHttpClient.get[VatJsonRoot]("/vat/vrn/123456")).thenReturn(Some(vatRoot))

      val result = connector.root("/vat/vrn/123456")

      result shouldBe vatRoot
    }

    "return a root object with an empty set of links for a 404 response" in new VatConnectorApplication {

      when(mockHttpClient.get[VatJsonRoot]("/vat/vrn/123456")).thenReturn(None)
      connector.root("/vat/vrn/123456") shouldBe VatJsonRoot(Map.empty)
    }

    "Propagate any exception that gets thrown" in new VatConnectorApplication {
      val rootUri = "/vat/111456111"

      when(mockHttpClient.get[VatJsonRoot](rootUri)).thenThrow(new MicroServiceException("exception thrown by external service", mock[Response]))

      evaluating(connector.root(rootUri)) should produce[MicroServiceException]
    }
  }

  "VatConnector account summary" should {

    "call the micro service with the correct uri and return the contents" in new VatConnectorApplication {

      val accountSummary = Some(VatAccountSummary(Some(VatAccountBalance(Some(4.0))), None))
      when(mockHttpClient.get[VatAccountSummary]("/vat/vrn/123456/accountSummary")).thenReturn(accountSummary)

      val result = connector.accountSummary("/vat/vrn/123456/accountSummary")

      result shouldBe accountSummary
    }
  }

  "Requesting the calendar" should {

    "return a list of calendar events" in new VatConnectorApplication {
      val vatCalendarUri = "/vat/someVrn/calendar"

      val event1 = CalendarEvent(
        AccountingPeriod(new LocalDate(2013, 8, 20), new LocalDate(2014, 8, 19), true),
        new LocalDate(2013, 12, 15),
        "filing"
      )

      val event2 = CalendarEvent(
        AccountingPeriod(new LocalDate(2013, 8, 20), new LocalDate(2014, 8, 19), false),
        new LocalDate(2014, 2, 15),
        "payment"
      )

      when(mockHttpClient.get[List[CalendarEvent]](vatCalendarUri)).thenReturn(Some(List(event1, event2)))

      connector.calendar(vatCalendarUri).get shouldBe List(event1, event2)
    }

    "return the empty list if there are no events" in new VatConnectorApplication {
      val vatCalendarUri = "/vat/someVrn/calendar"

      when(mockHttpClient.get[List[CalendarEvent]](vatCalendarUri)).thenReturn(Some(List.empty[CalendarEvent]))

      connector.calendar(vatCalendarUri).get shouldBe List.empty[CalendarEvent]

    }
  }
}

class VatConnectorApplication extends WithApplication(FakeApplication()) with MockitoSugar {

  val mockHttpClient = mock[HttpWrapper]

  val connector = new VatConnector {
    override def httpGet[A](uri: String)(implicit m: Manifest[A]): Option[A] = mockHttpClient.get[A](uri)
  }

  class HttpWrapper {
    def get[T](uri: String): Option[T] = None
  }

}
