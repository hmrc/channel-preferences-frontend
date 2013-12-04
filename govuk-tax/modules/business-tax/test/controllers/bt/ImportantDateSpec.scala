package controllers.bt

import uk.gov.hmrc.common.BaseSpec
import org.joda.time.LocalDate
import views.formatting.Dates
import views.helpers.{InternalLink, PortalLink}
import uk.gov.hmrc.domain.{AccountingPeriod, CalendarEvent}

class ImportantDateSpec extends BaseSpec {

  private def buildPortalUrl(link: String): String = "someUrl"

  "Translating a filing calendar event into an important date" should {

    "return an important date object with additional text" in {
      val event = CalendarEventWithShowLink(
        CalendarEvent(
          AccountingPeriod(new LocalDate(2013, 1, 1), new LocalDate(2013, 12, 31), returnFiled = true),
          new LocalDate(2013, 9, 10),
          "FILING", "CT"),
        false)

      val expectedResult = ImportantDate("ct", "filing",
        new LocalDate(2013, 9, 10),
        "ct.message.importantDates.additionalText.filing",
        Seq(Dates.formatDate(new LocalDate(2013, 1, 1)), Dates.formatDate(new LocalDate(2013, 12, 31))),
        Some("ct.message.importantDates.text.filing")
      )

      val result = ImportantDate.create(event, buildPortalUrl)(null)
      result shouldBe expectedResult
    }

    "return an important date object with link but without additional text " in {
      val event = CalendarEventWithShowLink(
        CalendarEvent(
          AccountingPeriod(new LocalDate(2013, 1, 1), new LocalDate(2013, 12, 31), returnFiled = false),
          new LocalDate(2013, 9, 10),
          "FILING", "CT"),
        true)

      val expectedResult = ImportantDate("ct", "filing",
        new LocalDate(2013, 9, 10),
        "ct.message.importantDates.text.filing",
        Seq(Dates.formatDate(new LocalDate(2013, 1, 1)), Dates.formatDate(new LocalDate(2013, 12, 31))),
        None,
        Some(PortalLink("someUrl")),
        Some("ct-filing-href")
      )

      val result = ImportantDate.create(event, buildPortalUrl)(null)

      result shouldBe expectedResult
    }

    "return an important date object with no link and no additional text " in {
      val event = CalendarEventWithShowLink(
        CalendarEvent(
          AccountingPeriod(new LocalDate(2013, 1, 1), new LocalDate(2013, 12, 31), returnFiled = false),
          new LocalDate(2013, 9, 10),
          "FILING", "CT"),
        false)

      val expectedResult = ImportantDate("ct", "filing",
        new LocalDate(2013, 9, 10),
        "ct.message.importantDates.text.filing",
        Seq(Dates.formatDate(new LocalDate(2013, 1, 1)), Dates.formatDate(new LocalDate(2013, 12, 31))),
        None,
        None
      )

      val result = ImportantDate.create(event, buildPortalUrl)(null)

      result shouldBe expectedResult
    }

    "CT - Translating a payment calendar event into an important date" should {

      "return an important date object with link" in {
        val event = CalendarEventWithShowLink(
          CalendarEvent(
            AccountingPeriod(new LocalDate(2013, 1, 1), new LocalDate(2013, 12, 31), returnFiled = false),
            new LocalDate(2013, 9, 10),
            "PAYMENT", "CT"),
          true)

        val expectedResult = ImportantDate("ct", "payment",
          new LocalDate(2013, 9, 10),
          "ct.message.importantDates.text.payment",
          Seq(Dates.formatDate(new LocalDate(2013, 1, 1)), Dates.formatDate(new LocalDate(2013, 12, 31))),
          None,
          Some(InternalLink(routes.PaymentController.makeCtPayment().url)),
          Some("ct-payment-href")
        )

        val result = ImportantDate.create(event, buildPortalUrl)(null)
        result shouldBe expectedResult
      }
      "return an important date object with no link" in {
        val event = CalendarEventWithShowLink(
          CalendarEvent(
            AccountingPeriod(new LocalDate(2013, 1, 1), new LocalDate(2013, 12, 31), returnFiled = false),
            new LocalDate(2013, 9, 10),
            "PAYMENT", "CT"),
          false)

        val expectedResult = ImportantDate("ct", "payment",
          new LocalDate(2013, 9, 10),
          "ct.message.importantDates.text.payment",
          Seq(Dates.formatDate(new LocalDate(2013, 1, 1)), Dates.formatDate(new LocalDate(2013, 12, 31))),
          None,
          None
        )

        val result = ImportantDate.create(event, buildPortalUrl)(null)
        result shouldBe expectedResult
      }
    }

    "VAT - Translating a payment calendar event into an important date" should {

      "return an important date object for payment type payment-directdebit (text is displayed in grey, we have additional message and no link is present)" in {
        val event = CalendarEventWithShowLink(CalendarEvent(
          AccountingPeriod(new LocalDate(2013, 1, 1), new LocalDate(2013, 12, 31), returnFiled = false),
          new LocalDate(2013, 9, 10),
          "payment-directdebit", "VAT"), false)

        val expectedResult = ImportantDate("vat", "payment-directdebit",
          new LocalDate(2013, 9, 10),
          "vat.message.importantDates.additionalText.payment-directdebit",
          Seq(Dates.formatDate(new LocalDate(2013, 1, 1)), Dates.formatDate(new LocalDate(2013, 12, 31))),
          Some("vat.message.importantDates.text.payment-directdebit"),
          None
        )

        val result = ImportantDate.create(event, buildPortalUrl)(null)
        result shouldBe expectedResult
      }

      "return an important date object for payment type payment-cheque (only text is displayed)" in {
        val event = CalendarEventWithShowLink(CalendarEvent(
          AccountingPeriod(new LocalDate(2013, 1, 1), new LocalDate(2013, 12, 31), returnFiled = false),
          new LocalDate(2013, 9, 10),
          "payment-cheque", "VAT"), false)

        val expectedResult = ImportantDate("vat", "payment-cheque",
          new LocalDate(2013, 9, 10),
          "vat.message.importantDates.text.payment-cheque",
          Seq(Dates.formatDate(new LocalDate(2013, 1, 1)), Dates.formatDate(new LocalDate(2013, 12, 31))),
          None,
          None
        )

        val result = ImportantDate.create(event, buildPortalUrl)(null)
        result shouldBe expectedResult
      }

      "return an important date object for payment type payment-card (text and make a payment link are displayed)" in {
        val event = CalendarEventWithShowLink(CalendarEvent(
          AccountingPeriod(new LocalDate(2013, 1, 1), new LocalDate(2013, 12, 31), returnFiled = false),
          new LocalDate(2013, 9, 10),
          "payment-card", "VAT"), true)

        val expectedResult = ImportantDate("vat", "payment-card",
          new LocalDate(2013, 9, 10),
          "vat.message.importantDates.text.payment-card",
          Seq(Dates.formatDate(new LocalDate(2013, 1, 1)), Dates.formatDate(new LocalDate(2013, 12, 31))),
          None,
          Some(InternalLink(routes.PaymentController.makeVatPayment().url)),
          Some("vat-payment-card-href")
        )

        val result = ImportantDate.create(event, buildPortalUrl)(null)
        result shouldBe expectedResult
      }

      "return an important date object for payment type payment-card (text is the only one been displayed)" in {
        val event = CalendarEventWithShowLink(CalendarEvent(
          AccountingPeriod(new LocalDate(2013, 1, 1), new LocalDate(2013, 12, 31), returnFiled = false),
          new LocalDate(2013, 9, 10),
          "payment-card", "VAT"), false)

        val expectedResult = ImportantDate("vat", "payment-card",
          new LocalDate(2013, 9, 10),
          "vat.message.importantDates.text.payment-card",
          Seq(Dates.formatDate(new LocalDate(2013, 1, 1)), Dates.formatDate(new LocalDate(2013, 12, 31))),
          None,
          None,
          Some("vat-payment-card-href")
        )

        val result = ImportantDate.create(event, buildPortalUrl)(null)
        result shouldBe expectedResult
      }

      "return an important date object for payment type payment-online (text and make a payment link are displayed) " in {
        val event = CalendarEventWithShowLink(CalendarEvent(
          AccountingPeriod(new LocalDate(2013, 1, 1), new LocalDate(2013, 12, 31), returnFiled = false),
          new LocalDate(2013, 9, 10),
          "payment-online", "VAT"), true)

        val expectedResult = ImportantDate("vat", "payment-online",
          new LocalDate(2013, 9, 10),
          "vat.message.importantDates.text.payment-online",
          Seq(Dates.formatDate(new LocalDate(2013, 1, 1)), Dates.formatDate(new LocalDate(2013, 12, 31))),
          None,
          Some(InternalLink(routes.PaymentController.makeVatPayment().url)),
          Some("vat-payment-online-href")
        )

        val result = ImportantDate.create(event, buildPortalUrl)(null)
        result shouldBe expectedResult
      }

      "return an important date object for payment type payment-online (text is the only one been displayed) " in {
        val event = CalendarEventWithShowLink(CalendarEvent(
          AccountingPeriod(new LocalDate(2013, 1, 1), new LocalDate(2013, 12, 31), returnFiled = false),
          new LocalDate(2013, 9, 10),
          "payment-online", "VAT"), false)

        val expectedResult = ImportantDate("vat", "payment-online",
          new LocalDate(2013, 9, 10),
          "vat.message.importantDates.text.payment-online",
          Seq(Dates.formatDate(new LocalDate(2013, 1, 1)), Dates.formatDate(new LocalDate(2013, 12, 31))),
          None,
          None,
          Some("vat-payment-online-href")
        )

        val result = ImportantDate.create(event, buildPortalUrl)(null)
        result shouldBe expectedResult
      }
    }
  }

}
