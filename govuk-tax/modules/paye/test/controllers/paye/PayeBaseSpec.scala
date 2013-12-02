package controllers.paye

import uk.gov.hmrc.common.BaseSpec
import java.net.URI
import uk.gov.hmrc.common.microservice.paye.domain._
import org.joda.time.{DateTime, LocalDate}
import uk.gov.hmrc.common.microservice.auth.domain.UserAuthority
import uk.gov.hmrc.common.microservice.paye.domain.PayeRoot
import uk.gov.hmrc.common.microservice.paye.domain.Employment._
import uk.gov.hmrc.common.microservice.auth.domain.Regimes
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.common.microservice.domain.RegimeRoots
import uk.gov.hmrc.common.microservice.paye.domain.Benefit
import uk.gov.hmrc.common.microservice.paye.domain.TaxCode
import org.joda.time.chrono.ISOChronology
import uk.gov.hmrc.common.microservice.txqueue.domain.{Status, TxQueueTransaction}
import BenefitTypes._

trait PayeBaseSpec extends BaseSpec {

  import controllers.domain.AuthorityUtils._
  lazy val testTaxYear = 2013

  val currentTestDate = new DateTime(testTaxYear - 1, 12, 2, 12, 1, ISOChronology.getInstanceUTC)

  def defaultTxLinks(nino: String) = Map("accepted" -> s"/txqueue/current-status/paye/$nino/ACCEPTED/after/{from}",
    "completed" -> s"/txqueue/current-status/paye/$nino/COMPLETED/after/{from}",
    "failed" -> s"/txqueue/current-status/paye/$nino/FAILED/after/{from}",
    "findByOid" -> "/txqueue/oid/{oid}")

  def defaultActions(nino: String) = Map("calculateBenefitValue" -> "/calculation/paye/benefit/new/value-calculation")

  private def setupUser(id: String, nino: String, name: String): User = {
    setupUser(id, nino, name, defaultTxLinks(nino), defaultActions(nino), testTaxYear)
  }

  private def setupUser(id: String, nino: String, name: String, transactionLinks: Map[String, String], actions: Map[String, String], year:Int): User = {

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
        "taxCode" -> s"/paye/$nino/tax-codes/$year",
        "employments" -> s"/paye/$nino/employments/$year",
        "benefits" -> s"/paye/$nino/benefits/$year",
        "addBenefits" -> s"/paye/$nino/benefits/$year"),
      transactionLinks = transactionLinks,
      actions = actions
    )

    User(
      userId = id,
      userAuthority = payeAuthority(id, nino),
      regimes = RegimeRoots(paye = Some(payeRoot)),
      nameFromGovernmentGateway = None,
      decryptedToken = None
    )
  }

  val carBenefitEmployer1 = Benefit(31, testTaxYear, 321.42, 1, None, None, None, None, None, None, None,
    Some(Car(Some(new LocalDate(testTaxYear - 1, 12, 12)), None, Some(new LocalDate(testTaxYear - 1, 12, 12)), Some(0), Some("diesel"), Some(124), Some(1400), None, Some(BigDecimal("12343.21")), None, None)), actions("AB123456C", testTaxYear, 1), Map.empty)

  val fuelBenefitEmployer1 = Benefit(29, testTaxYear, 22.22, 1, None, None, None, None, None, None, None,
    None, actions("AB123456C", testTaxYear, 1), Map.empty)

  val johnDensmoresBenefitsForEmployer1 = Seq(
    carBenefitEmployer1,
    fuelBenefitEmployer1)

  val johnDensmoreOid = "jdensmore"

  val johnDensmore = setupUser(s"/auth/oid/$johnDensmoreOid", "AB123456C", "John Densmore")

  val userWithRemovedCar = setupUser("/auth/oid/removedCar", "RC123456B", "User With Removed Car")

  val johnDensmoresTaxCodes = Seq(TaxCode(1, Some(1), testTaxYear, "430L", List(Allowance(1000, 2000, 11))))

  def johnDensmoresOneEmployment(sequenceNumberVal: Int = 1) = Seq(
    Employment(sequenceNumber = sequenceNumberVal, startDate = new LocalDate(testTaxYear, 7, 2), endDate = Some(new LocalDate(testTaxYear, 10, 8)), taxDistrictNumber = "898", payeNumber = "9900112", employerName = Some("Weyland-Yutani Corp"), employmentType = primaryEmploymentType))

  val johnDensmoresEmployments = Seq(
    Employment(sequenceNumber = 1, startDate = new LocalDate(testTaxYear, 7, 2), endDate = Some(new LocalDate(testTaxYear, 10, 8)), taxDistrictNumber = "898", payeNumber = "9900112", employerName = Some("Weyland-Yutani Corp"), employmentType = primaryEmploymentType),
    Employment(sequenceNumber = 2, startDate = new LocalDate(testTaxYear, 10, 14), endDate = None, taxDistrictNumber = "899", payeNumber = "1212121", employerName =None, employmentType = 2))

  val carBenefit = Benefit(31, testTaxYear, 321.42, 2, None, None, None, None, None, None, None,
    Some(Car(Some(new LocalDate(testTaxYear -1 , 12, 12)), None, Some(new LocalDate(testTaxYear -1, 12, 12)), Some(0), Some("diesel"), Some(124), Some(1400), None, Some(BigDecimal("12343.21")), None, None)), actions("AB123456C", testTaxYear, 1), Map("withdraw" -> s"/paye/C123456/benefit/withdraw/2000/$testTaxYear-05-30/withdrawDate"))

  val fuelBenefit = Benefit( 29, testTaxYear, 22.22, 2, None, None, None, None, None, None, None,
    None, actions("AB123456C", testTaxYear, 1), Map("withdraw" -> s"/paye/C123456/benefit/withdraw/2000/$testTaxYear-09-10/withdrawDate"))

  val carAndFuelBenefitWithDifferentEmploymentNumbers = Seq(
    Benefit(29, testTaxYear, 135.33, 1, None, None, None, None, None, None, None, None, Map.empty, Map.empty),
    carBenefit)

  val johnDensmoresBenefits = Seq(
    Benefit(30, testTaxYear, 135.33, 1, None, None, None, None, None, None, None, None, Map.empty, Map.empty),
    fuelBenefit,
    carBenefit)

  val removedCarBenefit = Benefit(31, testTaxYear, 321.42, 1, None, None, None, None, None, None, None,
    Some(Car(None, Some(new LocalDate(testTaxYear, 7, 12)), Some(new LocalDate(testTaxYear - 1, 12, 12)), Some(0), Some("diesel"), Some(124), Some(1400), None, Some(BigDecimal("12343.21")), None, None)), actions("RC123456B", testTaxYear, 1), Map.empty)

  val userWithRemovedCarEmployments = Some(Seq(
    Employment(sequenceNumber = 1, startDate = new LocalDate(testTaxYear, 7, 2), endDate = Some(new LocalDate(testTaxYear, 10, 8)), taxDistrictNumber = "898", payeNumber = "9900112", employerName = Some("Sansbury"), employmentType = primaryEmploymentType),
    Employment(sequenceNumber = 2, startDate = new LocalDate(testTaxYear, 10, 14), endDate = None, taxDistrictNumber = "899", payeNumber = "1212121", employerName = None, employmentType = 2)))

  val userWithRemovedCarBenefits = Some(Seq(
    Benefit(29, testTaxYear, 22.22, 3, None, None, None, None, None, None, None, None, actions("RC123456B", testTaxYear, 1), Map.empty),
    removedCarBenefit))

  def transactionWithTags(tags: List[String], properties: Map[String, String] = Map.empty, employmentSequenceNumber: Int = 1) =
    TxQueueTransaction(URI.create("http://tax.com"),
      "paye",
      URI.create("http://tax.com"),
      None,
      List(Status("created", None, currentTestDate)),
      Some(tags),
      properties ++ Map("employmentSequenceNumber" -> employmentSequenceNumber.toString, "taxYear" -> s"$testTaxYear"),
      currentTestDate,
      currentTestDate.minusDays(1))

  val removedCarTransaction = transactionWithTags(List("paye", "test", "message.code.removeBenefits"), Map("benefitTypes" -> s"$CAR"))
  val otherTransaction = transactionWithTags(List("paye", "test"))
  val removedFuelTransaction = transactionWithTags(List("paye", "test", "message.code.removeBenefits"), Map("benefitTypes" -> s"$FUEL"))
  val removedFuelTransactionForEmployment2 = transactionWithTags(List("paye", "test", "message.code.removeBenefits"), Map("benefitTypes" -> s"$FUEL"), 2)
  val multiBenefitTransaction = transactionWithTags(List("paye", "test", "message.code.removeBenefits"), Map("benefitTypes" -> s"$CAR,$FUEL"))

  val addCarTransaction = transactionWithTags(List("paye", "test", "message.code.addBenefits"), Map("benefitTypes" -> s"$CAR"))
  val addFuelTransaction = transactionWithTags(List("paye", "test", "message.code.addBenefits"), Map("benefitTypes" -> s"$FUEL"))

  val testTransactions = List(removedCarTransaction, otherTransaction, removedFuelTransaction)

  val multiBenefitTransactions = List(multiBenefitTransaction)

  val completedTransactions = List(otherTransaction, removedFuelTransaction)

  val acceptedTransactions = List(removedCarTransaction)

  protected def actions(nino: String, year: Int, esn: Int): Map[String, String] = {
    Map("remove" -> s"/paye/$nino/benefits/$year/$esn/update")
  }

}
