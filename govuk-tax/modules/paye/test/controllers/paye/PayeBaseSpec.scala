package controllers.paye

import uk.gov.hmrc.common.BaseSpec
import java.net.URI
import uk.gov.hmrc.common.microservice.paye.domain._
import org.joda.time.{DateTime, LocalDate}
import uk.gov.hmrc.common.microservice.paye.domain.PayeRoot
import uk.gov.hmrc.common.microservice.paye.domain.Employment._
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.common.microservice.domain.RegimeRoots
import uk.gov.hmrc.common.microservice.paye.domain.Benefit
import uk.gov.hmrc.common.microservice.paye.domain.TaxCode
import org.joda.time.chrono.ISOChronology
import uk.gov.hmrc.common.microservice.txqueue.domain.{Status, TxQueueTransaction}
import BenefitTypes._
import play.api.test.FakeRequest
import models.paye.{ReplaceCarBenefitFormData, CarBenefitData, RemoveCarBenefitFormData}
import controllers.common.SessionKeys

object QuestionableConversions {
  implicit def valueToQuestionable[A](a: A) = Questionable(a, displayable = true)
  implicit def questionableToValue[A](q: Questionable[A]): A = q.value
}

trait PayeBaseSpec extends BaseSpec  with TaxYearSupport {

  import controllers.domain.AuthorityUtils._

  import QuestionableConversions._

  lazy val testTaxYear = currentTaxYear

  val currentTestDate = new DateTime(testTaxYear - 1, 12, 2, 12, 1, ISOChronology.getInstanceUTC)

  val bePresent = not be null

  object LocalDate {
    def apply(year: Int, month: Int, day: Int) = new LocalDate(year, month, day, ISOChronology.getInstanceUTC)

    def apply() = new LocalDate()
  }

  def defaultTxLinks(nino: String) = Map(
    "history" -> s"/txqueue/current-status/paye/$nino/history/after/{from}?statuses={statuses}&max-results={maxResults}",
    "findByOid" -> "/txqueue/oid/{oid}")

  def defaultActions(nino: String) = Map("calculateBenefitValue" -> "/calculation/paye/benefit/new/value-calculation")

  private def setupUser(id: String, nino: String, name: String): User = {
    setupUser(id, nino, name, defaultTxLinks(nino), defaultActions(nino), testTaxYear)
  }

  private def setupUser(id: String, nino: String, name: String, transactionLinks: Map[String, String], actions: Map[String, String], year: Int): User = {

    val payeRoot = PayeRoot(
      name = name,
      firstName = "Barney",
      secondName = None,
      surname = "Rubble",
      nino = nino,
      title = "Mr",
      dateOfBirth = "1976-04-12",
      links = Map(
        "taxCode" -> s"/paye/$nino/tax-codes/$year",
        "employments" -> s"/paye/$nino/employments/$year",
        "benefit-cars" -> s"/paye/$nino/benefit-cars/$year",
        "benefits" -> s"/paye/$nino/benefits/$year",
        "version" -> s"/paye/$nino/version"),
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

  val johnDensmoresCarBenefitData = new CarBenefitData(providedFrom = Some(new LocalDate(testTaxYear, 7, 29)),
    carRegistrationDate = Some(new LocalDate(1950, 9, 13)),
    listPrice = Some(1000),
    employeeContributes = Some(true),
    employeeContribution = Some(100),
    privateUsePayment = Some(true),
    privateUsePaymentAmount = Some(999),
    fuelType = Some("electricity"),
    co2Figure = None,
    co2NoFigure = None,
    engineCapacity = Some("2000"),
    employerPayFuel = Some("false"),
    dateFuelWithdrawn = None)
  val carGrossAmount = BigDecimal(321.42)


  val fuelGrossAmount = BigDecimal(22.22)
  val carBenefitEmployer1 = Benefit(31, testTaxYear, carGrossAmount, 1, None, None, None, None, None, None, None,
    Some(Car(Some(LocalDate(testTaxYear - 1, 12, 12)), None, LocalDate(testTaxYear - 1, 12, 12), Some(0),
      Some("diesel"), Some(124), Some(1400), None, Some(BigDecimal("12343.21")), Some(BigDecimal("120.21")), None)),
    actions("AB123456C", testTaxYear, 1), Map.empty, benefitAmount = Some(264.42))

  val fuelBenefitEmployer1 = Benefit(29, testTaxYear, fuelGrossAmount, 1, None, None, None, None, None, None, None,
    None, actions("AB123456C", testTaxYear, 1), Map.empty, benefitAmount = Some(5.22))

  val johnDensmoresBenefitsForEmployer1 = Seq(CarBenefit(carBenefitEmployer1, Some(fuelBenefitEmployer1)))

  val johnDensmoreOid = "jdensmore"

  val johnDensmore = setupUser(s"/auth/oid/$johnDensmoreOid", "AB123456C", "John Densmore")

  val johnDensmoreVersionNumber = 22

  def requestWithCorrectVersion = FakeRequest().withSession((SessionKeys.npsVersion, johnDensmoreVersionNumber.toString))

  def requestWithIncorrectVersion = FakeRequest().withSession((SessionKeys.npsVersion, "20"))

  val userWithRemovedCar = setupUser("/auth/oid/removedCar", "RC123456B", "User With Removed Car")

  val johnDensmoresTaxCodes = Seq(TaxCode(1, Some(1), testTaxYear, "430L", List(Allowance(1000, 2000, 11))))

  def johnDensmoresOneEmployment(sequenceNumberVal: Int = 1) = Seq(
    Employment(sequenceNumber = sequenceNumberVal, startDate = LocalDate(testTaxYear, 7, 2), endDate = Some(LocalDate(testTaxYear, 10, 8)), taxDistrictNumber = "898", payeNumber = "9900112", employerName = Some("Weyland-Yutani Corp"), employmentType = primaryEmploymentType))

  val johnDensmoresEmployments = Seq(
    Employment(sequenceNumber = 1, startDate = LocalDate(testTaxYear, 7, 2), endDate = Some(LocalDate(testTaxYear, 10, 8)), taxDistrictNumber = "898", payeNumber = "9900112", employerName = Some("Weyland-Yutani Corp"), employmentType = primaryEmploymentType),
    Employment(sequenceNumber = 2, startDate = LocalDate(testTaxYear, 10, 14), endDate = None, taxDistrictNumber = "899", payeNumber = "1212121", employerName = None, employmentType = 2))

  val carBenefit = Benefit(31, testTaxYear, 321.42, 2, None, None, None, None, None, None, None,
    Some(Car(Some(LocalDate(testTaxYear - 1, 12, 12)), None, LocalDate(testTaxYear - 1, 12, 12), Some(0), Some("diesel"), Some(124), Some(1400), None, Some(BigDecimal("12343.21")), None, None)), actions("AB123456C", testTaxYear, 1), Map("withdraw" -> s"/paye/C123456/benefit/withdraw/2000/$testTaxYear-05-30/withdrawDate"))

  val fuelBenefit = Benefit(29, testTaxYear, 22.22, 2, None, None, None, None, None, None, None,
    None, actions("AB123456C", testTaxYear, 1), Map("withdraw" -> s"/paye/C123456/benefit/withdraw/2000/$testTaxYear-09-10/withdrawDate"))

  val withdrawnFuelBenefit = Benefit(29, testTaxYear, 22.22, 2, None, None, None, None, None, None, Some(LocalDate()),
    None, actions("AB123456C", testTaxYear, 1), Map("withdraw" -> s"/paye/C123456/benefit/withdraw/2000/$testTaxYear-09-10/withdrawDate"))

  val carAndFuelBenefitWithDifferentEmploymentNumbers = Seq(CarAndFuel(carBenefit,
    Some(Benefit(29, testTaxYear, 135.33, 1, None, None, None, None, None, None, None, None, Map.empty, Map.empty))))

  val johnDensmoresBenefits = Seq(CarBenefit(carBenefit, Some(fuelBenefit)))

  val johnDensmoresRemovedCarBenefitFormData = new RemoveCarBenefitFormData(LocalDate(testTaxYear+1, 1, 8), Some(true), Some(10), Some(true), Some(2000), Some("differentDateFuel"), Some(LocalDate(testTaxYear, 2, 8)))

  val johnDensmoresReplaceCarBenefitData = ReplaceCarBenefitFormData(johnDensmoresRemovedCarBenefitFormData, johnDensmoresCarBenefitData)
  val johnDensmoresNewCarData = AddCarBenefitConfirmationData("employerName", LocalDate(testTaxYear+1, 1, 8), 1234, "diesel", Some(222), Some("2000"),
    Some(true), Some(50), Some(LocalDate(2000, 1, 1)), Some(300))


  val removedCarBenefit = Benefit(31, testTaxYear, 321.42, 1, None, None, None, None, None, None, None,
    Some(Car(None, Some(LocalDate(testTaxYear, 7, 12)), LocalDate(testTaxYear - 1, 12, 12), Some(0), Some("diesel"), Some(124), Some(1400), None, Some(BigDecimal("12343.21")), None, None)), actions("RC123456B", testTaxYear, 1), Map.empty)

  val userWithRemovedCarEmployments = Some(Seq(
    Employment(sequenceNumber = 1, startDate = LocalDate(testTaxYear, 7, 2), endDate = Some(LocalDate(testTaxYear, 10, 8)), taxDistrictNumber = "898", payeNumber = "9900112", employerName = Some("Sansbury"), employmentType = primaryEmploymentType),
    Employment(sequenceNumber = 2, startDate = LocalDate(testTaxYear, 10, 14), endDate = None, taxDistrictNumber = "899", payeNumber = "1212121", employerName = None, employmentType = 2)))

  val userWithRemovedCarBenefits = Some(Seq(
    Benefit(29, testTaxYear, 22.22, 3, None, None, None, None, None, None, None, None, actions("RC123456B", testTaxYear, 1), Map.empty),
    removedCarBenefit))

  def transactionWithTags(tags: List[String], properties: Map[String, String] = Map.empty, employmentSequenceNumber: Int = 1, mostRecentStatus: String) =
    TxQueueTransaction(URI.create("http://tax.com"),
      "paye",
      URI.create("http://tax.com"),
      None,
      List(Status(mostRecentStatus, None, currentTestDate),
        Status("created", None, currentTestDate)),
      Some(tags),
      properties ++ Map("employmentSequenceNumber" -> employmentSequenceNumber.toString, "taxYear" -> s"$testTaxYear"),
      currentTestDate,
      currentTestDate.minusDays(1))

  val acceptedRemovedCarTransaction = transactionWithTags(List("paye", "test", "message.code.removeBenefits"), Map("benefitTypes" -> s"$CAR"), mostRecentStatus = "accepted")
  val completedRemovedCarTransaction = transactionWithTags(List("paye", "test", "message.code.removeBenefits"), Map("benefitTypes" -> s"$CAR"), mostRecentStatus = "completed")
  val acceptedOtherTransaction = transactionWithTags(List("paye", "test"), mostRecentStatus = "accepted")
  val acceptedRemovedFuelTransaction = transactionWithTags(List("paye", "test", "message.code.removeBenefits"), Map("benefitTypes" -> s"$FUEL"), mostRecentStatus = "accepted")
  val completedRemovedFuelTransaction = transactionWithTags(List("paye", "test", "message.code.removeBenefits"), Map("benefitTypes" -> s"$FUEL"), mostRecentStatus = "completed")

  val acceptedAddCarTransaction = transactionWithTags(List("paye", "test", "message.code.addBenefits"), Map("benefitTypes" -> s"$CAR"), mostRecentStatus = "accepted")
  val completedAddCarTransaction = transactionWithTags(List("paye", "test", "message.code.addBenefits"), Map("benefitTypes" -> s"$CAR"), mostRecentStatus = "completed")
  val acceptedAddFuelTransaction = transactionWithTags(List("paye", "test", "message.code.addBenefits"), Map("benefitTypes" -> s"$FUEL"), mostRecentStatus = "accepted")
  val completedAddFuelTransaction = transactionWithTags(List("paye", "test", "message.code.addBenefits"), Map("benefitTypes" -> s"$FUEL"), mostRecentStatus = "completed")

  protected def actions(nino: String, year: Int, esn: Int): Map[String, String] = {
    Map("remove" -> s"/paye/$nino/benefits/$year/$esn/update")
  }

  object CarBenefitDataBuilder {
    val taxYear = testTaxYear
    val employmentSeqNumberOne = 1

    implicit class RichBoolean(val b: Boolean) {
      final def option[A](a: => A): Option[A] = if (b) Some(a) else None
    }

    val now: LocalDate = new LocalDate(taxYear, 10, 3)
    val inTwoDaysTime = now.plusDays(2)
    val inThreeDaysTime = now.plusDays(3)
    val endOfTaxYearMinusOne = new LocalDate(taxYear + 1, 4, 4)
    val defaultListPrice = 1000
    val defaultEmployeeContributes = false
    val defaultEmployeeContribution = None
    val defaultEmployerContributes = false
    val defaultEmployerContribution = None
    val defaultCarUnavailable = false
    val defaultNumberOfDaysUnavailable = None
    val defaultGiveBackThisTaxYear = false
    val defaultFuelType = "diesel"
    val defaultProvidedTo = None
    val defaultProvidedFrom = now.plusDays(2)
    val defaultCarRegistrationDate = now.minusYears(1)
    val defaultCo2Figure = None
    val defaultCo2NoFigure = true
    val defaultEngineCapacity = 1400
    val defaultEmployerPayFuel = "false"
    val defaultDateFuelWithdrawn = None

    def apply(providedFrom: Option[LocalDate] = Some(defaultProvidedFrom),
              carRegistrationDate: Option[LocalDate] = Some(defaultCarRegistrationDate),
              listPrice: Option[Int] = Some(defaultListPrice),
              employeeContributes: Option[Boolean] = Some(defaultEmployeeContributes),
              employeeContribution: Option[Int] = defaultEmployeeContribution,
              employerContributes: Option[Boolean] = Some(defaultEmployerContributes),
              employerContribution: Option[Int] = defaultEmployerContribution,
              fuelType: Option[String] = Some(defaultFuelType),
              co2Figure: Option[Int] = defaultCo2Figure,
              co2NoFigure: Option[Boolean] = Some(defaultCo2NoFigure),
              engineCapacity: Option[String] = Some(defaultEngineCapacity.toString),
              employerPayFuel: Option[String] = Some(defaultEmployerPayFuel),
              dateFuelWithdrawn: Option[LocalDate] = defaultDateFuelWithdrawn) = {

      CarBenefitData(providedFrom = providedFrom,
        carRegistrationDate = carRegistrationDate,
        listPrice = listPrice,
        employeeContributes = employeeContributes,
        employeeContribution = employeeContribution,
        privateUsePayment = employerContributes,
        privateUsePaymentAmount = employerContribution,
        fuelType = fuelType,
        co2Figure = co2Figure,
        co2NoFigure = co2NoFigure,
        engineCapacity = engineCapacity,
        employerPayFuel = employerPayFuel,
        dateFuelWithdrawn = dateFuelWithdrawn)
    }
  }


}