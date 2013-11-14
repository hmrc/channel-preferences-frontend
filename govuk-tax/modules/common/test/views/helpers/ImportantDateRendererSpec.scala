package views.helpers

import uk.gov.hmrc.common.BaseSpec
import play.api.test.{FakeApplication, WithApplication}
import org.joda.time.LocalDate
import java.text.SimpleDateFormat


class ImportantDateRendererSpec extends BaseSpec {


  "render an important date message with the full year format" should {
    implicit val dateFormat = new SimpleDateFormat("d MMMM yyy")

    "render the message containing dates, text and link" in new WithApplication(FakeApplication()) {

      val message = ImportantDateMessage(
        RenderableDateMessage(new LocalDate(2013, 9, 10)),
        "some text to show",
        RenderableDateMessage(new LocalDate(2013, 11, 10)),
        RenderableDateMessage(new LocalDate(2014, 11, 9)),
        Some(RenderableLinkMessage(LinkMessage.internalLink("someUrl", "Make a payment")))
      )

      val result = message.render.toString().trim
      result shouldBe "<DIV class=\"activity-list__date\"><P>10 September 2013</P></DIV><DIV class=\"activity-list__description\"><P><SPAN>some text to show 10 November 2013 to 9 November 2014 is due</SPAN>." +
        "\n<A href=\"someUrl\">Make a payment</A></P></DIV>"
    }

    "render the message containing dates, grayed out text, no link and post message text" in new WithApplication(FakeApplication()) {

      val message = ImportantDateMessage(
        RenderableDateMessage(new LocalDate(2013, 9, 10)),
        "some text to show",
        RenderableDateMessage(new LocalDate(2013, 11, 10)),
        RenderableDateMessage(new LocalDate(2014, 11, 9)),
        None
      )

      val result = message.render.toString().trim

      result shouldBe "<DIV class=\"activity-list__date\"><P>10 September 2013</P></DIV><DIV class=\"activity-list__description\"><P><SPAN class=\"faded-text\">some text to show 10 November 2013 to 9 November 2014 is due</SPAN>" +
        " ct.message.importantDates.returnAlreadyReceived.</P></DIV>"
    }
  }


  "render an important date message with the short year format" should {
    implicit val dateFormat = new SimpleDateFormat("d MMMM")

    "render the message containing dates, text and link" in new WithApplication(FakeApplication()) {

      val message = ImportantDateMessage(
        RenderableDateMessage(new LocalDate(2013, 9, 10)),
        "some text to show",
        RenderableDateMessage(new LocalDate(2013, 11, 10)),
        RenderableDateMessage(new LocalDate(2014, 11, 9)),
        Some(RenderableLinkMessage(LinkMessage.internalLink("someUrl", "Make a payment")))
      )

      val result = message.render.toString().trim

      result shouldBe "<DIV class=\"activity-list__date\"><P>10 September</P></DIV><DIV class=\"activity-list__description\"><P><SPAN>some text to show 10 November to 9 November is due</SPAN>." +
        "\n<A href=\"someUrl\">Make a payment</A></P></DIV>"
    }

    "render the message containing dates, grayed out text, no link and post message text" in new WithApplication(FakeApplication()) {

      val message = ImportantDateMessage(
        RenderableDateMessage(new LocalDate(2013, 9, 10)),
        "some text to show",
        RenderableDateMessage(new LocalDate(2013, 11, 10)),
        RenderableDateMessage(new LocalDate(2014, 11, 9)),
        None
      )

      val result = message.render.toString().trim

      result shouldBe "<DIV class=\"activity-list__date\"><P>10 September</P></DIV><DIV class=\"activity-list__description\"><P><SPAN class=\"faded-text\">some text to show 10 November to 9 November is due</SPAN>" +
        " ct.message.importantDates.returnAlreadyReceived.</P></DIV>"
    }
  }

}
