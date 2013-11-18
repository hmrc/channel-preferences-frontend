package controllers.bt

import uk.gov.hmrc.common.BaseSpec
import org.scalatest.mock.MockitoSugar
import play.api.test.{FakeRequest, FakeApplication, WithApplication}
import uk.gov.hmrc.common.microservice.ct.CtConnector
import uk.gov.hmrc.common.microservice.ct.domain.{AccountingPeriod, CalendarEvent, CtRoot}
import uk.gov.hmrc.domain.CtUtr
import uk.gov.hmrc.common.microservice.domain.{RegimeRoots, User}
import uk.gov.hmrc.common.microservice.auth.domain.{Regimes, UserAuthority}
import org.mockito.Mockito._
import scala.concurrent.Future
import play.api.test.Helpers._
import org.jsoup.Jsoup
import org.joda.time.LocalDate
import uk.gov.hmrc.utils.DateTimeUtils
import controllers.bt.testframework.mocks.PortalUrlBuilderMock

class ImportantDatesControllerSpec extends BaseSpec with MockitoSugar {

  "important dates page" should {
    "render view with CT payment and filing dates" in new WithApplication(FakeApplication()) with PortalUrlBuilderMock {
      val currentYear = DateTimeUtils.now.getYear
      val regime = Some(CtRoot(CtUtr("someCtUtr"), Map("calendar"->"/ct/someCtUtr/calendar")))
      val user = User(userId = "userId", userAuthority = UserAuthority("userId", Regimes()),
        nameFromGovernmentGateway = Some("Ciccio"), regimes = RegimeRoots(ct = regime), decryptedToken = None)

      val mockCtConnector = mock[CtConnector]
      val controller = new ImportantDatesController(mockCtConnector, null)(null) with MockedPortalUrlBuilder

      val events = getSampleEvents

      when(mockPortalUrlBuilder.buildPortalUrl("ctFileAReturn")).thenReturn("someUrl")
      when(mockCtConnector.calendar("/ct/someCtUtr/calendar")).thenReturn(Some(events))

      val response = Future.successful(controller.importantDatesPage(user, FakeRequest()))

      verify(mockCtConnector).calendar("/ct/someCtUtr/calendar")
      verify(mockPortalUrlBuilder, times(1)).buildPortalUrl("ctFileAReturn")
      status(response) shouldBe 200

      val page = Jsoup.parse(contentAsString(response))
      val ul = page.getElementsByClass("activity-list").get(0)
      val dates = ul.getElementsByClass("activity-list__date")
      dates.size shouldBe 4
      dates.get(0).getElementsByTag("p").get(0).html shouldBe s"10 May ${currentYear}"
      dates.get(1).getElementsByTag("p").get(0).html shouldBe s"10 June ${currentYear}"
      dates.get(2).getElementsByTag("p").get(0).html shouldBe s"10 June ${currentYear+1}"
      dates.get(3).getElementsByTag("p").get(0).html shouldBe s"10 July ${currentYear+1}"

      ul.getElementsByClass("faded-text").size shouldBe 1
      val description = ul.getElementsByClass("activity-list__description")
      description.size shouldBe 4
    }

    "render view with no dates if there are none" in new WithApplication(FakeApplication()) with PortalUrlBuilderMock {

      val regime = Some(CtRoot(CtUtr("someCtUtr"), Map("calendar"->"/ct/someCtUtr/calendar")))
      val user = User(userId = "userId", userAuthority = UserAuthority("userId", Regimes()),
        nameFromGovernmentGateway = Some("Ciccio"), regimes = RegimeRoots(ct = regime), decryptedToken = None)

      val mockCtConnector = mock[CtConnector]
      val controller = new ImportantDatesController(mockCtConnector, null)(null) with MockedPortalUrlBuilder

      when(mockCtConnector.calendar("/ct/someCtUtr/calendar")).thenReturn(Some(List.empty))

      val response = Future.successful(controller.importantDatesPage(user, FakeRequest()))

      verify(mockCtConnector).calendar("/ct/someCtUtr/calendar")
      verifyZeroInteractions(mockPortalUrlBuilder)
      status(response) shouldBe 200
     
      val page = Jsoup.parse(contentAsString(response))
      val ul = page.getElementsByClass("activity-list").get(0)
      ul.getElementsByClass("activity-List__date").size shouldBe 0
    }
  }

  def getSampleEvents = {
    val currentYear = DateTimeUtils.now.getYear
    val event1 = CalendarEvent(
      AccountingPeriod(new LocalDate(currentYear, 2, 2), new LocalDate(currentYear + 1, 1, 2), true),
      new LocalDate(currentYear, 5, 10),
      "filing"
    )

    val event2 = CalendarEvent(
      AccountingPeriod(new LocalDate(currentYear+ 1 , 2, 2), new LocalDate(currentYear + 2, 1, 2), false),
      new LocalDate(currentYear + 1, 6, 10),
      "filing"
    )

    val event3 = CalendarEvent(
      AccountingPeriod(new LocalDate(currentYear, 2, 2), new LocalDate(currentYear + 1, 1, 2), false),
      new LocalDate(currentYear, 6, 10),
      "payment"
    )

    val event4 = CalendarEvent(
      AccountingPeriod(new LocalDate(currentYear + 1, 2, 2), new LocalDate(currentYear + 2, 1, 2), false),
      new LocalDate(currentYear + 1, 7, 10),
      "payment"
    )
    List(event1, event2, event3, event4)
  }

}
