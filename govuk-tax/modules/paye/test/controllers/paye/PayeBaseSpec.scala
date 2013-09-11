package controllers.paye

import uk.gov.hmrc.common.BaseSpec
import uk.gov.hmrc.microservice.domain.{ RegimeRoots, User }
import uk.gov.hmrc.microservice.auth.domain.{ Regimes, UserAuthority }
import java.net.URI
import uk.gov.hmrc.microservice.paye.domain._
import org.joda.time.{ DateTime, LocalDate }
import uk.gov.hmrc.microservice.auth.domain.UserAuthority
import uk.gov.hmrc.microservice.paye.domain.PayeRoot
import scala.Some
import uk.gov.hmrc.microservice.paye.domain.Employment
import uk.gov.hmrc.microservice.auth.domain.Regimes
import uk.gov.hmrc.microservice.domain.User
import uk.gov.hmrc.microservice.domain.RegimeRoots
import uk.gov.hmrc.microservice.paye.domain.Benefit
import uk.gov.hmrc.microservice.paye.domain.TaxCode
import uk.gov.hmrc.microservice.txqueue.{ Status, TxQueueTransaction }
import org.joda.time.format.DateTimeFormat

class PayeBaseSpec extends BaseSpec {

  val currentTestDate = new DateTime()
  val dateFormat = DateTimeFormat.forPattern("yyyy-MM-dd")

  protected def setupUser(id: String, nino: String, name: String): User = {
    val ua = UserAuthority(s"/personal/paye/$nino", Regimes(paye = Some(URI.create(s"/personal/paye/$nino"))), None)

    val payeRoot = PayeRoot(
      name = name,
      firstName = "Barney",
      secondName = None,
      surname = "Rubble",
      nino = nino,
      version = 22,
      title = "Mr",
      dateOfBirth = "1976-04-12",
      links = Map(
        "taxCode" -> s"/paye/$nino/tax-codes/2013",
        "employments" -> s"/paye/$nino/employments/2013",
        "benefits" -> s"/paye/$nino/benefits/2013"),
      transactionLinks = Map("accepted" -> s"/txqueue/current-status/paye/$nino/ACCEPTED/after/{from}",
        "completed" -> s"/txqueue/current-status/paye/$nino/COMPLETED/after/{from}",
        "failed" -> s"/txqueue/current-status/paye/$nino/FAILED/after/{from}",
        "findByOid" -> "/txqueue/oid/{oid}")
    )

    User(id, ua, RegimeRoots(Some(payeRoot), None, None), None, None)
  }

  val johnDensmore = setupUser("/auth/oid/jdensmore", "AB123456C", "John Densmore")

  val userWithRemovedCar = setupUser("/auth/oid/removedCar", "RC123456B", "User With Removed Car")

  val johnDensmoresTaxCodes = Seq(TaxCode(1, 2013, "430L"))

  val johnDensmoresOneEmployment = Seq(
    Employment(sequenceNumber = 1, startDate = new LocalDate(2013, 7, 2), endDate = Some(new LocalDate(2013, 10, 8)), taxDistrictNumber = "898", payeNumber = "9900112", employerName = Some("Weyland-Yutani Corp")))

  val johnDensmoresEmployments = Seq(
    Employment(sequenceNumber = 1, startDate = new LocalDate(2013, 7, 2), endDate = Some(new LocalDate(2013, 10, 8)), taxDistrictNumber = "898", payeNumber = "9900112", employerName = Some("Weyland-Yutani Corp")),
    Employment(sequenceNumber = 2, startDate = new LocalDate(2013, 10, 14), endDate = None, taxDistrictNumber = "899", payeNumber = "1212121", employerName = None))

  val carBenefit = Benefit(benefitType = 31, taxYear = 2013, grossAmount = 321.42, employmentSequenceNumber = 2, null, null, null, null, null, null,
    car = Some(Car(None, None, Some(new LocalDate(2012, 12, 12)), 0, 2, 124, 1, "B", BigDecimal("12343.21"))), actions("AB123456C", 2013, 1), Map.empty)

  val fuelBenefit = Benefit(benefitType = 29, taxYear = 2013, grossAmount = 22.22, employmentSequenceNumber = 2, null, null, null, null, null, null,
    car = None, actions("AB123456C", 2013, 1), Map.empty)

  val carAndFuelBenefitWithDifferentEmploymentNumbers = Seq(
    Benefit(benefitType = 29, taxYear = 2013, grossAmount = 135.33, employmentSequenceNumber = 1, null, null, null, null, null, null, car = None, Map.empty, Map.empty),
    carBenefit)

  val johnDensmoresBenefits = Seq(
    Benefit(benefitType = 30, taxYear = 2013, grossAmount = 135.33, employmentSequenceNumber = 1, null, null, null, null, null, null, car = None, Map.empty, Map.empty),
    fuelBenefit,
    carBenefit)

  val removedCarBenefit = Benefit(benefitType = 31, taxYear = 2013, grossAmount = 321.42, employmentSequenceNumber = 1, null, null, null, null, null, null,
    car = Some(Car(None, Some(new LocalDate(2013, 7, 12)), Some(new LocalDate(2012, 12, 12)), 0, 2, 124, 1, "B", BigDecimal("12343.21"))), actions("RC123456B", 2013, 1), Map.empty)

  val userWithRemovedCarEmployments = Some(Seq(
    Employment(sequenceNumber = 1, startDate = new LocalDate(2013, 7, 2), endDate = Some(new LocalDate(2013, 10, 8)), taxDistrictNumber = "898", payeNumber = "9900112", employerName = Some("Sansbury")),
    Employment(sequenceNumber = 2, startDate = new LocalDate(2013, 10, 14), endDate = None, taxDistrictNumber = "899", payeNumber = "1212121", employerName = None)))

  val userWithRemovedCarBenefits = Some(Seq(
    Benefit(benefitType = 29, taxYear = 2013, grossAmount = 22.22, employmentSequenceNumber = 3, null, null, null, null, null, null, car = None, actions("RC123456B", 2013, 1), Map.empty),
    removedCarBenefit))

  def transactionWithTags(tags: List[String], properties: Map[String, String] = Map.empty) =
    TxQueueTransaction(URI.create("http://tax.com"),
      "paye",
      URI.create("http://tax.com"),
      None,
      List(Status("created", None, currentTestDate)),
      Some(tags),
      properties ++ Map("employmentSequenceNumber" -> "1", "taxYear" -> "2013"),
      currentTestDate,
      currentTestDate.minusDays(1))

  val removedCarTransaction = transactionWithTags(List("paye", "test", "message.code.removeCarBenefits"), Map("benefitType" -> "31"))
  val otherTransaction = transactionWithTags(List("paye", "test"))
  val removedFuelTransaction = transactionWithTags(List("paye", "test", "message.code.removeFuelBenefits"), Map("benefitType" -> "29"))

  val testTransactions = List(removedCarTransaction, otherTransaction, removedFuelTransaction)

  val completedTransactions = List(otherTransaction, removedFuelTransaction)

  val acceptedTransactions = List(removedCarTransaction)

  private def actions(nino: String, year: Int, esn: Int): Map[String, String] = {
    Map("remove" -> s"/paye/$nino/benefits/$year/$esn/update/cars")
  }

}
