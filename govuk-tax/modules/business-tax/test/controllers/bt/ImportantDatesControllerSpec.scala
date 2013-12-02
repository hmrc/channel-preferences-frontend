package controllers.bt

import uk.gov.hmrc.common.BaseSpec
import org.scalatest.mock.MockitoSugar
import play.api.test.{FakeRequest, FakeApplication, WithApplication}
import uk.gov.hmrc.common.microservice.ct.CtConnector
import uk.gov.hmrc.common.microservice.ct.domain.CtRoot
import uk.gov.hmrc.domain.{AccountingPeriod, CalendarEvent, Vrn, CtUtr}
import uk.gov.hmrc.common.microservice.domain.{RegimeRoots, User}
import uk.gov.hmrc.common.microservice.auth.domain.{Regimes, UserAuthority}
import org.mockito.Mockito._
import scala.concurrent._
import ExecutionContext.Implicits.global
import play.api.test.Helpers._
import org.jsoup.Jsoup
import org.joda.time.LocalDate
import uk.gov.hmrc.utils.DateTimeUtils
import controllers.bt.testframework.mocks.PortalUrlBuilderMock
import uk.gov.hmrc.common.microservice.vat.VatConnector
import uk.gov.hmrc.common.microservice.vat.domain.VatRoot

class ImportantDatesControllerSpec extends BaseSpec with MockitoSugar {

  private val currentYear = DateTimeUtils.now.getYear
  private val ctCalendarUrl = "/ct/someCtUtr/calendar"
  private val vatCalendarUrl = "/vat/someVrn/calendar"

  private def ctAndVatUser = {
    val ctRegime = Some(CtRoot(CtUtr("someCtUtr"), Map("calendar" -> ctCalendarUrl)))
    val vatRegime = Some(VatRoot(Vrn("someVrn"), Map("calendar" -> vatCalendarUrl)))
    User(userId = "userId", userAuthority = UserAuthority("userId", Regimes()),
      nameFromGovernmentGateway = Some("Ciccio"), regimes = RegimeRoots(ct = ctRegime, vat = vatRegime), decryptedToken = None)
  }

  private def ctUser = {
    val ctRegime = Some(CtRoot(CtUtr("someCtUtr"), Map("calendar" -> ctCalendarUrl)))
    User(userId = "userId", userAuthority = UserAuthority("userId", Regimes()),
      nameFromGovernmentGateway = Some("Ciccio"), regimes = RegimeRoots(ct = ctRegime), decryptedToken = None)
  }

  "important dates page" should {

    "render view with CT payment and filing dates" in new WithApplication(FakeApplication()) with PortalUrlBuilderMock {
      val user = ctAndVatUser

      val mockCtConnector = mock[CtConnector]
      val mockVatConnector = mock[VatConnector]
      val controller = new ImportantDatesController(mockCtConnector, mockVatConnector, null)(null) with MockedPortalUrlBuilder

      val ctEvents = getCtSampleEvents
      val vatEvents = getVatSampleEvents
      val allEvents = (ctEvents ++ vatEvents).sortBy(_.eventDate.toDate)

      when(mockPortalUrlBuilder.buildPortalUrl("ctFileAReturn")).thenReturn("someUrl")
      when(mockCtConnector.calendar(ctCalendarUrl)).thenReturn(Some(ctEvents))
      when(mockVatConnector.calendar(vatCalendarUrl)).thenReturn(Some(vatEvents))
      when(mockPortalUrlBuilder.buildPortalUrl("vatFileAReturn")).thenReturn("someUrl")

      val response = controller.importantDatesPage(user, FakeRequest())

      status(response) shouldBe 200

      val page = Jsoup.parse(contentAsString(response))

      verify(mockCtConnector).calendar(ctCalendarUrl)
      verify(mockPortalUrlBuilder, times(1)).buildPortalUrl("ctFileAReturn")
      verify(mockVatConnector).calendar(vatCalendarUrl)
      verify(mockPortalUrlBuilder, times(1)).buildPortalUrl("vatFileAReturn")

      val ul = page.getElementsByClass("activity-list").get(0)
      val dates = ul.getElementsByClass("activity-list__date")
      dates.size shouldBe allEvents.length
      dates.get(0).getElementsByTag("p").get(0).html shouldBe s"10 May $currentYear"
      dates.get(1).getElementsByTag("p").get(0).html shouldBe s"10 June $currentYear"
      dates.get(2).getElementsByTag("p").get(0).html shouldBe s"10 August $currentYear"
      dates.get(3).getElementsByTag("p").get(0).html shouldBe s"10 June ${currentYear + 1}"
      dates.get(4).getElementsByTag("p").get(0).html shouldBe s"10 July ${currentYear + 1}"
      dates.get(5).getElementsByTag("p").get(0).html shouldBe s"10 September ${currentYear + 1}"

      ul.getElementsByClass("faded-text").size shouldBe 2
      val description = ul.getElementsByClass("activity-list__description")
      description.size shouldBe allEvents.length

    }

    "render view with no dates if there are none" in new WithApplication(FakeApplication()) with PortalUrlBuilderMock {
      val user = ctUser

      val mockCtConnector = mock[CtConnector]
      val controller = new ImportantDatesController(mockCtConnector, null, null)(null) with MockedPortalUrlBuilder

      when(mockCtConnector.calendar(ctCalendarUrl)).thenReturn(Future(Some(List.empty)))

      val response = Future.successful(controller.importantDatesPage(user, FakeRequest()))

      verify(mockCtConnector).calendar(ctCalendarUrl)
      verifyZeroInteractions(mockPortalUrlBuilder)
      status(response) shouldBe 200

      val page = Jsoup.parse(contentAsString(response))
      val ul = page.getElementsByClass("activity-list").get(0)
      ul.getElementsByClass("activity-List__date").size shouldBe 0
    }
  }

  def getCtSampleEvents = {
    val event1 = CalendarEvent(
      AccountingPeriod(new LocalDate(currentYear, 2, 2), new LocalDate(currentYear + 1, 1, 2), returnFiled = true),
      new LocalDate(currentYear, 5, 10),
      "filing",
      "CT"
    )

    val event2 = CalendarEvent(
      AccountingPeriod(new LocalDate(currentYear + 1, 2, 2), new LocalDate(currentYear + 2, 1, 2), returnFiled = false),
      new LocalDate(currentYear + 1, 6, 10),
      "filing",
      "CT"
    )

    val event3 = CalendarEvent(
      AccountingPeriod(new LocalDate(currentYear, 2, 2), new LocalDate(currentYear + 1, 1, 2), returnFiled = false),
      new LocalDate(currentYear, 6, 10),
      "payment",
      "CT"
    )

    val event4 = CalendarEvent(
      AccountingPeriod(new LocalDate(currentYear + 1, 2, 2), new LocalDate(currentYear + 2, 1, 2), returnFiled = false),
      new LocalDate(currentYear + 1, 7, 10),
      "payment",
      "CT"
    )
    List(event1, event2, event3, event4)
  }

  def getVatSampleEvents = {
    val event1 = CalendarEvent(
      AccountingPeriod(new LocalDate(currentYear, 2, 2), new LocalDate(currentYear + 1, 1, 2), returnFiled = false),
      new LocalDate(currentYear, 8, 10),
      "payment-directdebit",
      "VAT"
    )

    val event2 = CalendarEvent(
      AccountingPeriod(new LocalDate(currentYear + 1, 2, 2), new LocalDate(currentYear + 2, 1, 2), returnFiled = false),
      new LocalDate(currentYear + 1, 9, 10),
      "filing",
      "VAT"
    )
    List(event1, event2)
  }

}
