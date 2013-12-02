package controllers.bt

import uk.gov.hmrc.common.BaseSpec
import uk.gov.hmrc.domain.{AccountingPeriod, CalendarEvent}
import org.joda.time.LocalDate

class CalendarEventWithShowLinkSpec extends BaseSpec{

  "CalendarEventWithShowLink - addShowLinkToCalendarEvents" should {
    "return an empty list when an empty list of calendar events is sent as parameter" in {
      CalendarEventWithShowLink.addShowLinkToCalendarEvents(List.empty) shouldBe List.empty
    }
    "convert list that contains 3 pending filing events into a list with the 3 events but only the first one with the configuration to display the link" in {
      val filingEvent1 = CalendarEvent(AccountingPeriod(new LocalDate(2013, 1, 1), new LocalDate(2013, 2, 1), returnFiled = false), new LocalDate(2013, 2, 1), "filing", "anyregime")
      val filingEvent2 = CalendarEvent(AccountingPeriod(new LocalDate(2013, 3, 1), new LocalDate(2013, 4, 1), returnFiled = false), new LocalDate(2013, 4, 1), "filing", "anyregime")
      val filingEvent3 = CalendarEvent(AccountingPeriod(new LocalDate(2013, 5, 1), new LocalDate(2013, 6, 1), returnFiled = false), new LocalDate(2013, 6, 1), "filing", "anyregime")
      val parameter = List(filingEvent1, filingEvent2, filingEvent3)
      val result = CalendarEventWithShowLink.addShowLinkToCalendarEvents(parameter)
      result shouldBe List(CalendarEventWithShowLink(filingEvent1, showLink = true), CalendarEventWithShowLink(filingEvent2, showLink = false), CalendarEventWithShowLink(filingEvent3, showLink = false))
    }
    "convert list that contains 1 paid filing event and 2 pending filing events into a list with the 3 events but only the second one with the configuration to display the link" in {
      val filingEvent1 = CalendarEvent(AccountingPeriod(new LocalDate(2013, 1, 1), new LocalDate(2013, 2, 1), returnFiled = true), new LocalDate(2013, 2, 1), "filing", "anyregime")
      val filingEvent2 = CalendarEvent(AccountingPeriod(new LocalDate(2013, 3, 1), new LocalDate(2013, 4, 1), returnFiled = false), new LocalDate(2013, 4, 1), "filing", "anyregime")
      val filingEvent3 = CalendarEvent(AccountingPeriod(new LocalDate(2013, 5, 1), new LocalDate(2013, 6, 1), returnFiled = false), new LocalDate(2013, 6, 1), "filing", "anyregime")
      val parameter = List(filingEvent1, filingEvent2, filingEvent3)
      val result = CalendarEventWithShowLink.addShowLinkToCalendarEvents(parameter)
      result shouldBe List(CalendarEventWithShowLink(filingEvent1, showLink = false), CalendarEventWithShowLink(filingEvent2, showLink = true), CalendarEventWithShowLink(filingEvent3, showLink = false))
    }
    "convert list that contains 3 pending payment events into a list with the 3 events but only the first one with the configuration to display the link" in {
      val paymentEvent1 = CalendarEvent(AccountingPeriod(new LocalDate(2013, 1, 1), new LocalDate(2013, 2, 1), returnFiled = false), new LocalDate(2013, 2, 1), "payment", "anyregime")
      val paymentEvent2 = CalendarEvent(AccountingPeriod(new LocalDate(2013, 3, 1), new LocalDate(2013, 4, 1), returnFiled = false), new LocalDate(2013, 4, 1), "payment", "anyregime")
      val paymentEvent3 = CalendarEvent(AccountingPeriod(new LocalDate(2013, 5, 1), new LocalDate(2013, 6, 1), returnFiled = false), new LocalDate(2013, 6, 1), "payment", "anyregime")
      val parameter = List(paymentEvent1, paymentEvent2, paymentEvent3)
      val result = CalendarEventWithShowLink.addShowLinkToCalendarEvents(parameter)
      result shouldBe List(CalendarEventWithShowLink(paymentEvent1, showLink = true), CalendarEventWithShowLink(paymentEvent2, showLink = false), CalendarEventWithShowLink(paymentEvent3, showLink = false))
    }
    "convert list that contains 1 paid payment event and 2 pending payment events into a list with the 3 events but only the second one with the configuration to display the link" in {
      val paymentEvent1 = CalendarEvent(AccountingPeriod(new LocalDate(2013, 1, 1), new LocalDate(2013, 2, 1), returnFiled = true), new LocalDate(2013, 2, 1), "payment", "anyregime")
      val paymentEvent2 = CalendarEvent(AccountingPeriod(new LocalDate(2013, 3, 1), new LocalDate(2013, 4, 1), returnFiled = false), new LocalDate(2013, 4, 1), "payment", "anyregime")
      val paymentEvent3 = CalendarEvent(AccountingPeriod(new LocalDate(2013, 5, 1), new LocalDate(2013, 6, 1), returnFiled = false), new LocalDate(2013, 6, 1), "payment", "anyregime")
      val parameter = List(paymentEvent1, paymentEvent2, paymentEvent3)
      val result = CalendarEventWithShowLink.addShowLinkToCalendarEvents(parameter)
      result shouldBe List(CalendarEventWithShowLink(paymentEvent1, showLink = false), CalendarEventWithShowLink(paymentEvent2, showLink = true), CalendarEventWithShowLink(paymentEvent3, showLink = false))
    }
    "convert list that contains 3 pending payment-card events into a list with the 3 events but only the first one with the configuration to display the link" in {
      val paymentEvent1 = CalendarEvent(AccountingPeriod(new LocalDate(2013, 1, 1), new LocalDate(2013, 2, 1), returnFiled = false), new LocalDate(2013, 2, 1), "payment-card", "anyregime")
      val paymentEvent2 = CalendarEvent(AccountingPeriod(new LocalDate(2013, 3, 1), new LocalDate(2013, 4, 1), returnFiled = false), new LocalDate(2013, 4, 1), "payment-card", "anyregime")
      val paymentEvent3 = CalendarEvent(AccountingPeriod(new LocalDate(2013, 5, 1), new LocalDate(2013, 6, 1), returnFiled = false), new LocalDate(2013, 6, 1), "payment-card", "anyregime")
      val parameter = List(paymentEvent1, paymentEvent2, paymentEvent3)
      val result = CalendarEventWithShowLink.addShowLinkToCalendarEvents(parameter)
      result shouldBe List(CalendarEventWithShowLink(paymentEvent1, showLink = true), CalendarEventWithShowLink(paymentEvent2, showLink = false), CalendarEventWithShowLink(paymentEvent3, showLink = false))
    }
    "convert list that contains 1 paid payment-card event and 2 pending payment events into a list with the 3 events but only the second one with the configuration to display the link" in {
      val paymentEvent1 = CalendarEvent(AccountingPeriod(new LocalDate(2013, 1, 1), new LocalDate(2013, 2, 1), returnFiled = true), new LocalDate(2013, 2, 1), "payment-card", "anyregime")
      val paymentEvent2 = CalendarEvent(AccountingPeriod(new LocalDate(2013, 3, 1), new LocalDate(2013, 4, 1), returnFiled = false), new LocalDate(2013, 4, 1), "payment-card", "anyregime")
      val paymentEvent3 = CalendarEvent(AccountingPeriod(new LocalDate(2013, 5, 1), new LocalDate(2013, 6, 1), returnFiled = false), new LocalDate(2013, 6, 1), "payment-card", "anyregime")
      val parameter = List(paymentEvent1, paymentEvent2, paymentEvent3)
      val result = CalendarEventWithShowLink.addShowLinkToCalendarEvents(parameter)
      result shouldBe List(CalendarEventWithShowLink(paymentEvent1, showLink = false), CalendarEventWithShowLink(paymentEvent2, showLink = true), CalendarEventWithShowLink(paymentEvent3, showLink = false))
    }
    "convert list that contains 3 pending payment-online events into a list with the 3 events but only the first one with the configuration to display the link" in {
      val paymentEvent1 = CalendarEvent(AccountingPeriod(new LocalDate(2013, 1, 1), new LocalDate(2013, 2, 1), returnFiled = false), new LocalDate(2013, 2, 1), "payment-online", "anyregime")
      val paymentEvent2 = CalendarEvent(AccountingPeriod(new LocalDate(2013, 3, 1), new LocalDate(2013, 4, 1), returnFiled = false), new LocalDate(2013, 4, 1), "payment-online", "anyregime")
      val paymentEvent3 = CalendarEvent(AccountingPeriod(new LocalDate(2013, 5, 1), new LocalDate(2013, 6, 1), returnFiled = false), new LocalDate(2013, 6, 1), "payment-online", "anyregime")
      val parameter = List(paymentEvent1, paymentEvent2, paymentEvent3)
      val result = CalendarEventWithShowLink.addShowLinkToCalendarEvents(parameter)
      result shouldBe List(CalendarEventWithShowLink(paymentEvent1, showLink = true), CalendarEventWithShowLink(paymentEvent2, showLink = false), CalendarEventWithShowLink(paymentEvent3, showLink = false))
    }
    "convert list that contains 1 paid payment-online event and 2 pending payment events into a list with the 3 events but only the second one with the configuration to display the link" in {
      val paymentEvent1 = CalendarEvent(AccountingPeriod(new LocalDate(2013, 1, 1), new LocalDate(2013, 2, 1), returnFiled = true), new LocalDate(2013, 2, 1), "payment-online", "anyregime")
      val paymentEvent2 = CalendarEvent(AccountingPeriod(new LocalDate(2013, 3, 1), new LocalDate(2013, 4, 1), returnFiled = false), new LocalDate(2013, 4, 1), "payment-online", "anyregime")
      val paymentEvent3 = CalendarEvent(AccountingPeriod(new LocalDate(2013, 5, 1), new LocalDate(2013, 6, 1), returnFiled = false), new LocalDate(2013, 6, 1), "payment-online", "anyregime")
      val parameter = List(paymentEvent1, paymentEvent2, paymentEvent3)
      val result = CalendarEventWithShowLink.addShowLinkToCalendarEvents(parameter)
      result shouldBe List(CalendarEventWithShowLink(paymentEvent1, showLink = false), CalendarEventWithShowLink(paymentEvent2, showLink = true), CalendarEventWithShowLink(paymentEvent3, showLink = false))
    }
    "convert list that contains mixed events into list with only a link of each type (filing, payment, payment-card, payment-online)" in {
      val filingEvent1 = CalendarEvent(AccountingPeriod(new LocalDate(2013, 1, 1), new LocalDate(2013, 2, 1), returnFiled = true), new LocalDate(2013, 2, 1), "filing", "anyregime")
      val paymentEvent1 = CalendarEvent(AccountingPeriod(new LocalDate(2013, 1, 1), new LocalDate(2013, 2, 1), returnFiled = true), new LocalDate(2013, 2, 1), "payment", "anyregime")
      val paymentCardEvent1 = CalendarEvent(AccountingPeriod(new LocalDate(2013, 1, 1), new LocalDate(2013, 2, 1), returnFiled = true), new LocalDate(2013, 2, 1), "payment-card", "anyregime")
      val paymentOnlineEvent1 = CalendarEvent(AccountingPeriod(new LocalDate(2013, 1, 1), new LocalDate(2013, 2, 1), returnFiled = true), new LocalDate(2013, 2, 1), "payment-online", "anyregime")
      val filingEvent2 = CalendarEvent(AccountingPeriod(new LocalDate(2013, 3, 1), new LocalDate(2013, 4, 1), returnFiled = false), new LocalDate(2013, 4, 1), "filing", "anyregime")
      val paymentEvent2 = CalendarEvent(AccountingPeriod(new LocalDate(2013, 3, 1), new LocalDate(2013, 4, 1), returnFiled = false), new LocalDate(2013, 4, 1), "payment", "anyregime")
      val paymentCardEvent2 = CalendarEvent(AccountingPeriod(new LocalDate(2013, 3, 1), new LocalDate(2013, 4, 1), returnFiled = false), new LocalDate(2013, 4, 1), "payment-card", "anyregime")
      val paymentOnlineEvent2 = CalendarEvent(AccountingPeriod(new LocalDate(2013, 3, 1), new LocalDate(2013, 4, 1), returnFiled = false), new LocalDate(2013, 4, 1), "payment-online", "anyregime")
      val filingEvent3 = CalendarEvent(AccountingPeriod(new LocalDate(2013, 5, 1), new LocalDate(2013, 6, 1), returnFiled = false), new LocalDate(2013, 6, 1), "filing", "anyregime")
      val paymentEvent3 = CalendarEvent(AccountingPeriod(new LocalDate(2013, 5, 1), new LocalDate(2013, 6, 1), returnFiled = false), new LocalDate(2013, 6, 1), "payment", "anyregime")
      val paymentCardEvent3 = CalendarEvent(AccountingPeriod(new LocalDate(2013, 5, 1), new LocalDate(2013, 6, 1), returnFiled = false), new LocalDate(2013, 6, 1), "payment-card", "anyregime")
      val paymentOnlineEvent3 = CalendarEvent(AccountingPeriod(new LocalDate(2013, 5, 1), new LocalDate(2013, 6, 1), returnFiled = false), new LocalDate(2013, 6, 1), "payment-online", "anyregime")
      val parameter = List(filingEvent1, paymentEvent1, paymentCardEvent1, paymentOnlineEvent1, filingEvent2, paymentEvent2, paymentCardEvent2, paymentOnlineEvent2, filingEvent3, paymentEvent3, paymentCardEvent3, paymentOnlineEvent3)
      val result = CalendarEventWithShowLink.addShowLinkToCalendarEvents(parameter)
      result shouldBe List(
        CalendarEventWithShowLink(filingEvent1, showLink = false),
        CalendarEventWithShowLink(paymentEvent1, showLink = false),
        CalendarEventWithShowLink(paymentCardEvent1, showLink = false),
        CalendarEventWithShowLink(paymentOnlineEvent1, showLink = false),
        CalendarEventWithShowLink(filingEvent2, showLink = true),
        CalendarEventWithShowLink(paymentEvent2, showLink = true),
        CalendarEventWithShowLink(paymentCardEvent2, showLink = true),
        CalendarEventWithShowLink(paymentOnlineEvent2, showLink = true),
        CalendarEventWithShowLink(filingEvent3, showLink = false),
        CalendarEventWithShowLink(paymentEvent3, showLink = false),
        CalendarEventWithShowLink(paymentCardEvent3, showLink = false),
        CalendarEventWithShowLink(paymentOnlineEvent3, showLink = false)
      )
    }
  }
}