package controllers.bt

import uk.gov.hmrc.common.BaseSpec
import uk.gov.hmrc.common.microservice.ct.domain.{AccountingPeriod, CalendarEvent}
import org.joda.time.LocalDate
import views.formatting.Dates
import views.helpers.{RenderableLinkMessage, LinkMessage}

class ImportantDateSpec extends BaseSpec {

  private def buildPortalUrl(link: String): String = "someUrl"

  "Translating a calendar event into an important date" should {

    "return an important date object with additional text" in {
      val event = CalendarEvent(
        AccountingPeriod(new LocalDate(2013, 1, 1), new LocalDate(2013, 12, 31), true),
        new LocalDate(2013, 9, 10),
        "FILING")

      val expectedResult = ImportantDate(
        new LocalDate(2013, 9, 10),
        "ct.message.importantDates.additionalText.filing",
        Seq(Dates.formatDate(new LocalDate(2013, 1, 1)), Dates.formatDate(new LocalDate(2013, 12, 31))),
        Some("ct.message.importantDates.text.filing")
      )

      val result = ImportantDate.create(event, buildPortalUrl)(null)

      result shouldBe expectedResult
    }


    "return an important date object with no additional text" in {
      val event = CalendarEvent(
        AccountingPeriod(new LocalDate(2013, 1, 1), new LocalDate(2013, 12, 31), false),
        new LocalDate(2013, 9, 10),
        "FILING")

      val expectedResult = ImportantDate(
        new LocalDate(2013, 9, 10),
        "ct.message.importantDates.text.filing",
        Seq(Dates.formatDate(new LocalDate(2013, 1, 1)), Dates.formatDate(new LocalDate(2013, 12, 31))),
        None,
        Some(RenderableLinkMessage(LinkMessage("someUrl", "ct.message.importantDates.link.filing", None, false, None, true)))
      )

      val result = ImportantDate.create(event, buildPortalUrl)(null)

      result shouldBe expectedResult
    }
  }


}
