package models.paye

import uk.gov.hmrc.common.BaseSpec
import uk.gov.hmrc.common.microservice.paye.domain.{ TaxCode, Employment }
import org.joda.time.{ DateTime, LocalDate }
import uk.gov.hmrc.microservice.txqueue.{ Status, TxQueueTransaction }
import java.net.URI

class EmploymentViewsSpec extends BaseSpec {

  val taxYear = 2013

  val employmentSequenceNumber = 1

  val employments = Seq(
    Employment(employmentSequenceNumber, LocalDate.now(), None, "1234", "5678", None),
    Employment(2, LocalDate.now(), None, "4321", "8765", None)
  )

  val taxCodes = Seq(
    TaxCode(employmentSequenceNumber, taxYear, "B211"),
    TaxCode(2, taxYear, "L332")
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
