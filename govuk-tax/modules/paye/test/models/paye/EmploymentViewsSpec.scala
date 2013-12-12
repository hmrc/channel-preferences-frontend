package models.paye

import uk.gov.hmrc.common.BaseSpec
import uk.gov.hmrc.common.microservice.paye.domain.{BenefitTypes, Employment, TaxCode}
import org.joda.time.{ DateTime, LocalDate }
import java.net.URI
import uk.gov.hmrc.common.microservice.paye.domain.Employment._
import scala.Some
import uk.gov.hmrc.common.microservice.txqueue.domain.{Status, TxQueueTransaction}
import org.joda.time.chrono.ISOChronology
import org.scalatest.LoneElement
import scala.collection.mutable

class EmploymentViewsSpec extends BaseSpec with LoneElement {

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

  val interestingBenefitTypes = Set(BenefitTypes.FUEL, BenefitTypes.CAR)
  
  "EmploymentViews apply" should {

    "add a tax code recent change object with the correct created date if a benefit transaction is in a completed state" in {
      val created = new DateTime(2013, 11, 5, 20, 0, ISOChronology.getInstanceUTC)
      val views = EmploymentViews.createEmploymentViews(
        employments,
        taxCodes,
        taxYear,
        interestingBenefitTypes,
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
      val recentChange = views(0).recentChanges.loneElement
      recentChange should have ('timeUserRequestedChange (created.toLocalDate),
                                'messageCode ("removeBenefits.completed"),
                                'types (List("29")))
    }

    "not add any recent change objects if they are for unknown benefit types" in {
      val views = EmploymentViews.createEmploymentViews(
        employments,
        taxCodes,
        taxYear,

        Set(BenefitTypes.TELEPHONE),
        Seq(TxQueueTransaction(URI.create("/foo"), "paye", URI.create("/user"), None,
          List(Status("ACCEPTED", None, DateTime.now())),
          Some(List("benefits", "remove", "fuel", "message.code.removeBenefits")),
          Map("employmentSequenceNumber" -> "1", "taxYear" -> "2013", "benefitTypes" -> "29"),
          DateTime.now,
          DateTime.now)
        )
      )
      views should have size 2
      views(0).recentChanges shouldBe empty
      views(1).recentChanges shouldBe empty
    }
  }
}
