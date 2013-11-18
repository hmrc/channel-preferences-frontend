package uk.gov.hmrc.common.microservice.ct

import uk.gov.hmrc.common.BaseSpec
import org.scalatest.mock.MockitoSugar
import play.api.test.{FakeApplication, WithApplication}
import org.mockito.Mockito._
import uk.gov.hmrc.microservice.MicroServiceException
import play.api.libs.ws.Response
import uk.gov.hmrc.common.microservice.ct.domain.{AccountingPeriod, CalendarEvent, CtJsonRoot}
import org.joda.time.LocalDate

class CtConnectorSpec extends BaseSpec {

  "Requesting the CT root" should {

    "return the root object for a successful response" in new CtConnectorApplication {

      val ctRootUri = "/ct/1234512345"
      val root = CtJsonRoot(Map("someLink" -> "somePath", "someOtherLink" -> "someOTherPath"))

      when(mockHttpClient.get[CtJsonRoot](ctRootUri)).thenReturn(Some(root))

      connector.root(ctRootUri) shouldBe root
    }

    "return a root object with an empty set of links for a 404 response" in new CtConnectorApplication {
      val ctRootUri = "/ct/55555"
      val emptyRoot = CtJsonRoot(Map.empty)

      when(mockHttpClient.get[CtJsonRoot](ctRootUri)).thenReturn(None)

      connector.root(ctRootUri) shouldBe emptyRoot
    }

    "propagate any exception that gets thrown" in new CtConnectorApplication {
      val ctRootUri = "/ct/55555"

      when(mockHttpClient.get[CtJsonRoot](ctRootUri)).thenThrow(new MicroServiceException("exception thrown by external service", mock[Response]))

      evaluating(connector.root(ctRootUri)) should produce[MicroServiceException]
    }
  }

  "Requesting the CT calendar" should {
     "return a list of calendar events" in new CtConnectorApplication {
       val ctCalendarUri = "/ct/someCtUtr/calendar"

       val event1 = CalendarEvent(
          AccountingPeriod(new LocalDate(2013, 8, 20), new LocalDate(2014, 8, 19), true),
          new LocalDate(2013, 12, 15),
          "eventType1"
       )

       val event2 = CalendarEvent(
         AccountingPeriod(new LocalDate(2013, 8, 20), new LocalDate(2014, 8, 19), false),
         new LocalDate(2014, 2, 15),
         "eventType2"
       )

       when(mockHttpClient.get[List[CalendarEvent]](ctCalendarUri)).thenReturn(Some(List(event1, event2)))

       connector.calendar(ctCalendarUri).get shouldBe List(event1, event2)
     }

    "return the empty list if there are no events" in new CtConnectorApplication {
      val ctCalendarUri = "/ct/someCtUtr/calendar"

      when(mockHttpClient.get[List[CalendarEvent]](ctCalendarUri)).thenReturn(Some(List.empty[CalendarEvent]))

      connector.calendar(ctCalendarUri).get shouldBe List.empty[CalendarEvent]

    }
  }
}

abstract class CtConnectorApplication extends WithApplication(FakeApplication()) with MockitoSugar {
  
  val mockHttpClient = mock[HttpWrapper]
  
  val connector = new CtConnector {
    override def httpGet[A](uri: String)(implicit m: Manifest[A]): Option[A] = mockHttpClient.get[A](uri)
  }
  
  class HttpWrapper {
    def get[T](uri: String): Option[T] = None
  }
}
