package models.paye

import uk.gov.hmrc.common.BaseSpec
import uk.gov.hmrc.common.microservice.paye.domain.Employment
import org.joda.time.{ DateTime, LocalDate }
import java.net.URI
import uk.gov.hmrc.common.microservice.paye.domain.Employment._
import scala.Some
import uk.gov.hmrc.common.microservice.paye.domain.TaxCode
import uk.gov.hmrc.common.microservice.txqueue.domain.{Status, TxQueueTransaction}
import org.joda.time.chrono.ISOChronology

class EmploymentViewsSpec extends BaseSpec {

  val taxYear = 2013

  val employmentSequenceNumber = 1

  val employments = Seq(
    Employment(employmentSequenceNumber, LocalDate.now(), None, "1234", "5678", None, primaryEmploymentType),
    Employment(2, LocalDate.now(), None, "4321", "8765", None, 2)
  )

  val taxCodes = Seq(
    TaxCode(employmentSequenceNumber, Some(1), taxYear, "B211", List.empty),
    TaxCode(2, Some(2),  taxYear, "L332", List.empty)
  )

  "EmploymentViews apply" should {
    "add a tax code recent change object if a benefit transaction is in an accepted state" in {
      val views = EmploymentViews(
        employments,
        taxCodes,
        taxYear,
        Seq(TxQueueTransaction(URI.create("/foo"), "paye", URI.create("/user"), None,
          List(Status("ACCEPTED", None, DateTime.now())),
          Some(List("benefits", "remove", "fuel", "message.code.removeBenefits")),
          Map("employmentSequenceNumber" -> "1", "taxYear" -> "2013", "benefitTypes" -> "29"),
          DateTime.now,
          DateTime.now)
        ),
        List.empty
      )
      views should have size 2
      views(0).taxCodeChange should not be (None)
      views(0).taxCodeChange.get.messageCode should be("taxcode.accepted")
      views(1).taxCodeChange should be(None)
    }

    "add a tax code recent change object if a benefit transaction is in a completed state" in {
      val views = EmploymentViews(
        employments,
        taxCodes,
        taxYear,
        List.empty,
        Seq(TxQueueTransaction(URI.create("/foo"), "paye", URI.create("/user"), None,
          List(Status("ACCEPTED", None, DateTime.now())),
          Some(List("benefits", "remove", "fuel", "message.code.removeBenefits")),
          Map("employmentSequenceNumber" -> "1", "taxYear" -> "2013", "benefitTypes" -> "29"),
          DateTime.now,
          DateTime.now)
        )
      )
      views should have size 2
      views(0).taxCodeChange should not be (None)
      views(0).taxCodeChange.get.messageCode should be("taxcode.completed")
      views(1).taxCodeChange should be(None)
    }

    "add a tax code recent change object with the correct created date if a benefit transaction is in a completed state" in {
      val created = new DateTime(2013, 11, 5, 20, 0, ISOChronology.getInstanceUTC)
      val views = EmploymentViews(
        employments,
        taxCodes,
        taxYear,
        List.empty,
        Seq(TxQueueTransaction(URI.create("/foo"), "paye", URI.create("/user"), None,
          List(Status("COMPLETED", None, new DateTime(2013, 11, 8, 20, 0, ISOChronology.getInstanceUTC)),
               Status("ACCEPTED", None, created)
          ),
          Some(List("benefits", "remove", "fuel", "message.code.removeBenefits")),
          Map("employmentSequenceNumber" -> "1", "taxYear" -> "2013", "benefitTypes" -> "29"),
          created,
          new DateTime(2013, 11, 8, 20, 0, ISOChronology.getInstanceUTC))
        )
      )
      views(0).recentChanges should have size 1
      views(0).recentChanges(0) should have ('timeUserRequestedChange (created.toLocalDate))
    }

    "not add any recent change objects if no related benefit transactions exist" in {
      val views = EmploymentViews(
        employments,
        taxCodes,
        taxYear,
        List.empty,
        Seq.empty
      )
      views should have size 2
      views(0).taxCodeChange should be(None)
      views(1).taxCodeChange should be(None)
    }
  }
}
