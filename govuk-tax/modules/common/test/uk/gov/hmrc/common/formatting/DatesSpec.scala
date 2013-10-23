package views.formatting

import org.scalatest.{ Matchers, WordSpec }
import Dates._
import org.joda.time.{DateTime, LocalDate}
import org.scalatest.prop.TableDrivenPropertyChecks._
import org.joda.time.chrono.ISOChronology

class DatesSpec extends WordSpec with Matchers {

  val UTC = ISOChronology.getInstanceUTC

  "Calling formatDate with a LocalDate object" should {

    "return the formatted date" in {
      val date = new LocalDate(2010, 9, 22, UTC)
      val expected = dateFormat.print(date)
      formatDate(date) should equal(expected)
    }
  }

  "Calling formatDate with an Optional LocalDate object and a default" should {

    "format the date if the input is Some date" in {
      val date = Some(new LocalDate(1984, 3, 31, UTC))
      val expected = dateFormat.print(date.get)
      formatDate(date, "the default value") should equal(expected)
    }

    "return the default if the input is None" in {
      val date = None
      val expected = "the default value"
      formatDate(date, "the default value") should equal(expected)
    }

  }

  "formatDate " should {
    "correctly format given dates " in {
      val dateTable =
        Table(
          ("date", "expectedDateFormat"),
          (new LocalDate(2001, 3, 5, UTC), "5 March 2001"),
          (new LocalDate(1, 1, 1, UTC), "1 January 1"),
          (new LocalDate(999, 1, 1, UTC), "1 January 999"),
          (new LocalDate(2013, 10, 23, UTC), "23 October 2013"),
          (new LocalDate(9999, 12, 31, UTC), "31 December 9999"),
          (new LocalDate(10000, 12, 31, UTC), "31 December 10000")
        )
      forAll (dateTable) { (date : LocalDate, expectedDateFormat : String) =>
        formatDate(date) shouldBe expectedDateFormat
      }
    }
  }

  "formatEasyReadingTimestamp " should {
    "correctly format given dates " in {
      val dateTable =
        Table(
          // UTC internally to -> Lon externally.
          ("date", "expectedDateFormat"),
          (new DateTime(2013, 10, 23, 12, 30, UTC), "Wednesday 23 October, 2013 at 1:30PM"),
          (new DateTime(1899, 7, 3, 12, 30, UTC), "Monday 3 July, 1899 at 12:30PM")
        )
      forAll (dateTable) { (date : DateTime, expectedDateFormat : String) =>
        formatEasyReadingTimestamp(Some(date), "") shouldBe expectedDateFormat
      }
    }
  }

  "shortDate " should {
    "correctly format given dates " in {
      val dateTable =
        Table(
          ("date", "expectedDateFormat"),
          (new LocalDate(1899, 5, 5, UTC), "1899-05-05"),
          (new LocalDate(2013, 10, 23, UTC), "2013-10-23")
        )
      forAll (dateTable) { (date : LocalDate, expectedDateFormat : String) =>
        shortDate(date) shouldBe expectedDateFormat
      }
    }
  }
}
