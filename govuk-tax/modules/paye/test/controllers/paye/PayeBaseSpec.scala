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
import uk.gov.hmrc.microservice.txqueue.{Status, TxQueueTransaction}
import org.joda.time.chrono.ISOChronology

class PayeBaseSpec extends BaseSpec {

  val currentTestDate = new DateTime(2012, 12, 2, 12, 1, ISOChronology.getInstanceUTC)

  def defaultTxLinks(nino: String) = Map("accepted" -> s"/txqueue/current-status/paye/$nino/ACCEPTED/after/{from}",
    "completed" -> s"/txqueue/current-status/paye/$nino/COMPLETED/after/{from}",
    "failed" -> s"/txqueue/current-status/paye/$nino/FAILED/after/{from}",
    "findByOid" -> "/txqueue/oid/{oid}")

  def defaultActions(nino: String) = Map("calculateBenefitValue" -> "/calculation/paye/benefit/new/value-calculation")

  protected def setupUser(id: String, nino: String, name: String): User = {
    setupUser(id, nino, name, defaultTxLinks(nino), defaultActions(nino))
  }

  protected def setupUser(id: String, nino: String, name: String, transactionLinks: Map[String, String], actions: Map[String, String]): User = {

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
      transactionLinks = transactionLinks,
      actions = actions
    )

    User(
      userId = id,
      userAuthority = ua,
      regimes = RegimeRoots(paye = Some(payeRoot)),
      nameFromGovernmentGateway = None,
      decryptedToken = None
    )
  }

  val johnDensmoreOid = "jdensmore"

  val johnDensmore = setupUser(s"/auth/oid/$johnDensmoreOid", "AB123456C", "John Densmore")

  val userWithRemovedCar = setupUser("/auth/oid/removedCar", "RC123456B", "User With Removed Car")

  val johnDensmoresTaxCodes = Seq(TaxCode(1, Some(1), 2013, "430L", List(Allowance(1000, 2000, 11))))

  def johnDensmoresOneEmployment(sequenceNumberVal: Int = 1) = Seq(
    Employment(sequenceNumber = sequenceNumberVal, startDate = new LocalDate(2013, 7, 2), endDate = Some(new LocalDate(2013, 10, 8)), taxDistrictNumber = "898", payeNumber = "9900112", employerName = Some("Weyland-Yutani Corp"), employmentType = primaryEmploymentType))

  val johnDensmoresEmployments = Seq(
    Employment(sequenceNumber = 1, startDate = new LocalDate(2013, 7, 2), endDate = Some(new LocalDate(2013, 10, 8)), taxDistrictNumber = "898", payeNumber = "9900112", employerName = Some("Weyland-Yutani Corp"), employmentType = primaryEmploymentType),
    Employment(sequenceNumber = 2, startDate = new LocalDate(2013, 10, 14), endDate = None, taxDistrictNumber = "899", payeNumber = "1212121", employerName = None, employmentType = 2))

  val carBenefit = Benefit(31, 2013, 321.42, 2, null, null, null, null, null, null,
    Some(Car(Some(new LocalDate(2012, 12, 12)), None, Some(new LocalDate(2012, 12, 12)), Some(0), Some("diesel"), Some(124), Some(1400), None, Some(BigDecimal("12343.21")), None, None)), actions("AB123456C", 2013, 1), Map("withdraw" -> "/paye/C123456/benefit/withdraw/2000/2013-05-30/withdrawDate"))

  val fuelBenefit = Benefit( 29, 2013, 22.22, 2, null, null, null, null, null, null,
    None, actions("AB123456C", 2013, 1), Map("withdraw" -> "/paye/C123456/benefit/withdraw/2000/2013-09-10/withdrawDate"))

  val carAndFuelBenefitWithDifferentEmploymentNumbers = Seq(
    Benefit(29, 2013, 135.33, 1, null, null, null, null, null, null, None, Map.empty, Map.empty),
    carBenefit)

  val johnDensmoresBenefits = Seq(
    Benefit(30, 2013, 135.33, 1, null, null, null, null, null, null, None, Map.empty, Map.empty),
    fuelBenefit,
    carBenefit)

  val removedCarBenefit = Benefit(31, 2013, 321.42, 1, null, null, null, null, null, null,
    Some(Car(None, Some(new LocalDate(2013, 7, 12)), Some(new LocalDate(2012, 12, 12)), Some(0), Some("diesel"), Some(124), Some(1400), None, Some(BigDecimal("12343.21")), None, None)), actions("RC123456B", 2013, 1), Map.empty)

  val userWithRemovedCarEmployments = Some(Seq(
    Employment(sequenceNumber = 1, startDate = new LocalDate(2013, 7, 2), endDate = Some(new LocalDate(2013, 10, 8)), taxDistrictNumber = "898", payeNumber = "9900112", employerName = Some("Sansbury"), employmentType = primaryEmploymentType),
    Employment(sequenceNumber = 2, startDate = new LocalDate(2013, 10, 14), endDate = None, taxDistrictNumber = "899", payeNumber = "1212121", employerName = None, employmentType = 2)))

  val userWithRemovedCarBenefits = Some(Seq(
    Benefit(29, 2013, 22.22, 3, null, null, null, null, null, null, None, actions("RC123456B", 2013, 1), Map.empty),
    removedCarBenefit))

  def transactionWithTags(tags: List[String], properties: Map[String, String] = Map.empty, employmentSequenceNumber: Int = 1) =
    TxQueueTransaction(URI.create("http://tax.com"),
      "paye",
      URI.create("http://tax.com"),
      None,
      List(Status("created", None, currentTestDate)),
      Some(tags),
      properties ++ Map("employmentSequenceNumber" -> employmentSequenceNumber.toString, "taxYear" -> "2013"),
      currentTestDate,
      currentTestDate.minusDays(1))

  val removedCarTransaction = transactionWithTags(List("paye", "test", "message.code.removeBenefits"), Map("benefitTypes" -> "31"))
  val otherTransaction = transactionWithTags(List("paye", "test"))
  val removedFuelTransaction = transactionWithTags(List("paye", "test", "message.code.removeBenefits"), Map("benefitTypes" -> "29"))
  val removedFuelEmployment2Transaction = transactionWithTags(List("paye", "test", "message.code.removeBenefits"), Map("benefitTypes" -> "29"), 2)
  val multiBenefitTransaction = transactionWithTags(List("paye", "test", "message.code.removeBenefits"), Map("benefitTypes" -> "31,29"))

  val testTransactions = List(removedCarTransaction, otherTransaction, removedFuelTransaction)

  val multiBenefitTransactions = List(multiBenefitTransaction)

  val completedTransactions = List(otherTransaction, removedFuelTransaction)

  val acceptedTransactions = List(removedCarTransaction)

  protected def actions(nino: String, year: Int, esn: Int): Map[String, String] = {
    Map("remove" -> s"/paye/$nino/benefits/$year/$esn/update")
  }

}
