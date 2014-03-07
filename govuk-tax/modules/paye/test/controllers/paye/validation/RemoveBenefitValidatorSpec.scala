package controllers.paye.validation

import controllers.paye.{TaxYearSupport, PayeBaseSpec}
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.utils.DateConverter
import controllers.DateFieldsHelper
import play.api.data.Form
import play.api.data.Forms._
import controllers.paye.CarBenefitFormFields._
import controllers.paye.validation.RemoveBenefitValidator._
import play.api.test.{FakeApplication, FakeRequest, WithApplication}
import org.joda.time.{Interval, LocalDate}
import play.api.i18n.Messages
import models.paye.CarFuelBenefitDates
import scala.Some

class RemoveBenefitValidatorSpec  extends PayeBaseSpec with MockitoSugar with DateConverter with DateFieldsHelper with TaxYearSupport {

  val now = new LocalDate(currentTaxYear, 10, 2)
  val endOfTaxYear = new LocalDate(currentTaxYear, 4, 5)

  "validateMandatoryBoolean" should {

    case class DummyModel(carUnavailable: Option[Boolean])

    def dummyForm = {
      Form(
        mapping(
          "carUnavailable" -> validateMandatoryBoolean
        )(DummyModel.apply)(DummyModel.unapply))
    }

    "accept a false value" in {
      val form = bindFormWithValue(dummyForm, "carUnavailable", "false")
      form.hasErrors shouldBe false
    }

    "accept a true value" in {
      val form = bindFormWithValue(dummyForm, "carUnavailable", "true")
      form.hasErrors shouldBe false
    }

    "not accept a value that cannot be mapped into a boolean" in {
      val form = bindFormWithValue(dummyForm, "carUnavailable", "i-am-not-boolean")
      form.hasErrors shouldBe true
      form.errors("carUnavailable").map(err => Messages(err.message)) should contain ("error.boolean")
    }

    "not accept an empty value" in {
      val form = bindFormWithValue(dummyForm, "carUnavailable", "")
      form.hasErrors shouldBe true
      form.errors("carUnavailable").map(err => Messages(err.message)) should contain ("error.paye.answer_mandatory")
    }

  }

  "validateNumberOfDaysUnavailable for field NUMBER OF DAYS UNAVAILABLE" should {

    val carBenefitStartDate = new LocalDate(currentTaxYear, 6, 30)

    case class DummyModel(daysUnavailable: Option[Int])

    def dummyForm(values:RemoveCarBenefitFormDataValues, benefitStartDate: LocalDate = carBenefitStartDate) = {
      Form(
        mapping(
          "daysUnavailable" -> validateNumberOfDaysUnavailable(Some(values), benefitStartDate, taxYearInterval)
        )(DummyModel.apply)(DummyModel.unapply))
    }

    "reject a value that is less than 0" in new WithApplication {
      val form = bindFormWithValue(dummyForm(getValues(carUnavailableVal=Some("true"))), "daysUnavailable", "-1")
      form.hasErrors shouldBe true
      form.errors("daysUnavailable").map(err => Messages(err.message)) should contain ("Enter a number more than 0.")
    }

    "reject a value that is 0" in new WithApplication {
      val form = bindFormWithValue(dummyForm(getValues(carUnavailableVal=Some("true"))), "daysUnavailable", "0")
      form.hasErrors shouldBe true
      form.errors("daysUnavailable").map(err => Messages(err.message)) should contain ("Enter a number more than 0.")
    }

    "accept a correct value" in new WithApplication {
      val form = bindFormWithValue(dummyForm(getValues(carUnavailableVal=Some("true"))), "daysUnavailable", "  32")
      form.hasErrors shouldBe false
    }

    "reject when the carUnavailable flag is true, but no value is provided." in new WithApplication {
      val form = bindFormWithValue(dummyForm(getValues(carUnavailableVal=Some("true"))), "daysUnavailable", "")
      form.hasErrors shouldBe true
      form.errors("daysUnavailable").map(err => Messages(err.message)) should contain ("Enter the number of days the car was unavailable.")
    }

    "not throw when carUnavailable is not a valid boolean" in new WithApplication {
      bindFormWithValue(dummyForm(getValues(carUnavailableVal=Some("wibble"))), "daysUnavailable", "")
    }

    "reject when the value is bigger than the providedFrom -> providedTo range." in new WithApplication {
      val fromDate = new LocalDate(2012, 5, 30)
      val toDate = Some(new LocalDate(2012, 5, 31))
      val form = bindFormWithValue(dummyForm(getValues(carUnavailableVal=Some("true"), withdrawDateVal = toDate), fromDate), "daysUnavailable", "3")
      form.hasErrors shouldBe true
      form.errors("daysUnavailable").map(err => Messages(err.message)) should contain (s"The car can’t be unavailable for longer than the total number of days you’ve had it from 6 April $testTaxYear. Reduce the number of days unavailable or check the date you got the car.")
    }
    
    "reject when there is a value in the daysUnavailable field and the user has chosen for it not to be unavailable" in new WithApplication {
       val form = bindFormWithValue(dummyForm(getValues(carUnavailableVal=Some("false"))), "daysUnavailable", "2")
      form.hasErrors shouldBe true
      form.errors("daysUnavailable").map(_.message) should contain("error.paye.remove_car_benefit.question2.extra_days_unavailable")
    }
  }

  "validateEmployeeContribution" should {

    case class DummyModel(employeeContribution: Option[Int])

    def dummyForm(values: RemoveCarBenefitFormDataValues) = {
      Form(
        mapping(
          "employeeContribution" -> validateEmployeeContribution(Some(values))
        )(DummyModel.apply)(DummyModel.unapply))
    }

    "reject a value that is more than 99999" in new WithApplication {
      val form = bindFormWithValue(dummyForm(getValues(employeeContributesVal = Some("true"))), "employeeContribution", "100000")
      form.hasErrors shouldBe true
      form.errors("employeeContribution").map(err => Messages(err.message)) should contain ("Enter a number between £1 and £99,999.")
    }

    "reject a value that is less than 0" in new WithApplication {
      val form = bindFormWithValue(dummyForm(getValues(employeeContributesVal=Some("true"))), "employeeContribution", "-1")
      form.hasErrors shouldBe true
      form.errors("employeeContribution").map(err => Messages(err.message)) should contain ("Employee payment must be greater than zero if you have selected yes.")
    }

    "reject a value that is 0" in new WithApplication {
      val form = bindFormWithValue(dummyForm(getValues(employeeContributesVal=Some("true"))), "employeeContribution", "0")
      form.hasErrors shouldBe true
      form.errors("employeeContribution").map(err => Messages(err.message)) should contain ("Employee payment must be greater than zero if you have selected yes.")
    }

    "accept a correct value" in new WithApplication {
      val form = bindFormWithValue(dummyForm(getValues(employeeContributesVal=Some("true"))), "employeeContribution", " 3276  ")
      form.hasErrors shouldBe false
    }

    "not throw when employeeContributesVal is not a valid boolean" in new WithApplication(FakeApplication()) {
      bindFormWithValue(dummyForm(getValues(employeeContributesVal=Some("wibble"))), "employeeContribution", "1234")
    }

    "reject when the employeeContributes flag is true, but no value is provided" in new WithApplication {
      val form = bindFormWithValue(dummyForm(getValues(employeeContributesVal=Some("true"))), "employeeContribution", "")
      form.hasErrors shouldBe true
      form.errors("employeeContribution").map(err => Messages(err.message)) should contain ("Enter a number between £1 and £99,999.")
    }

    "reject when the employeeContributes flag is false, but a value is provided" in new WithApplication {
      val form = bindFormWithValue(dummyForm(getValues(employeeContributesVal=Some("false"))), "employeeContribution", "1234")
      form.hasErrors shouldBe true
      form.errors("employeeContribution").map(_.message) should contain ("error.paye.remove_car_benefit.question3.extra_employee_contribution")
    }

  }

  "validateFuelDate in this tax year with a different date for Fuel" should {

    val carBenefitStartDate = new LocalDate(currentTaxYear, 6, 30)
    val carBenefitEndDate = new LocalDate(currentTaxYear, 7, 30)
    val defaultDates = CarFuelBenefitDates(Some(carBenefitEndDate), Some(FUEL_DIFFERENT_DATE))

    case class DummyModel( dateFuelWithdrawn: Option[LocalDate])

    def dummyForm(dates: CarFuelBenefitDates = defaultDates, benefitStartDate: Option[LocalDate] = Some(carBenefitStartDate), taxYearInterval: Interval = taxYearInterval) = {
      Form(
        mapping(
          dateFuelWithdrawn -> validateFuelDate(dates, Some(carBenefitStartDate), taxYearInterval)
        )(DummyModel.apply)(DummyModel.unapply))
    }

    "accept a fuel withdrawn date equal to the car withdrawn date" in {
      val fuelDate = carBenefitEndDate
      val formWithFuelWithdrawn = buildDateFormField(dateFuelWithdrawn, Some((fuelDate.getYear.toString, fuelDate.getMonthOfYear.toString, fuelDate.getDayOfMonth.toString)))

      val form = dummyForm().bindFromRequest()(FakeRequest().withFormUrlEncodedBody(formWithFuelWithdrawn:_*))
      form.hasErrors shouldBe false
      form.value.get.dateFuelWithdrawn shouldBe Some(fuelDate)
    }

    "accept a fuel withdrawn date that is before the car withdrawn date" in {
      val fuelDate = carBenefitEndDate.minusDays(1)
      val formWithFuelWithdrawn = buildDateFormField(dateFuelWithdrawn, Some((fuelDate.getYear.toString, fuelDate.getMonthOfYear.toString, fuelDate.getDayOfMonth.toString)))

      val form = dummyForm().bindFromRequest()(FakeRequest().withFormUrlEncodedBody(formWithFuelWithdrawn:_*))
      form.hasErrors shouldBe false
      form.value.get.dateFuelWithdrawn shouldBe Some(fuelDate)
    }

    "reject a fuel withdrawn date that is after the car withdrawn date" in new WithApplication {
      val fuelDate = carBenefitEndDate.plusDays(1)
      val fuelWithdrawnDateAfterCarWithdrawn = buildDateFormField(dateFuelWithdrawn, Some((fuelDate.getYear.toString, fuelDate.getMonthOfYear.toString, fuelDate.getDayOfMonth.toString)))

      val form = dummyForm().bindFromRequest()(FakeRequest().withFormUrlEncodedBody(fuelWithdrawnDateAfterCarWithdrawn:_*))
      form.hasErrors shouldBe true
    }

    "reject a fuel withdrawn date that is before the car benefit starting date" in new WithApplication {
      val fuelDate = carBenefitStartDate.minusDays(1)
      val fuelWithdrawnBeforeCarBenefitStarted = buildDateFormField(dateFuelWithdrawn, Some((fuelDate.getYear.toString, fuelDate.getMonthOfYear.toString, fuelDate.getDayOfMonth.toString)))

      val form = dummyForm().bindFromRequest()(FakeRequest().withFormUrlEncodedBody(fuelWithdrawnBeforeCarBenefitStarted:_*))
      form.hasErrors shouldBe true
    }

    "reject a fuel withdrawn date that is not in the current tax year" in new WithApplication {
      val fuelWithdrawnBeforeCarBenefitStarted = buildDateFormField(dateFuelWithdrawn, Some(("2010", "6", "7")))
      val form = dummyForm().bindFromRequest()(FakeRequest().withFormUrlEncodedBody(fuelWithdrawnBeforeCarBenefitStarted:_*))
      form.hasErrors shouldBe true
      form.errors(dateFuelWithdrawn).map(err => Messages(err.message)) should contain(s"Enter a date between 6 April $currentTaxYear and 5 April ${currentTaxYear+1}.")
    }

    "reject a fuel withdrawn date that is empty" in new WithApplication {
      val fuelWithdrawnBeforeCarBenefitStarted = buildDateFormField(dateFuelWithdrawn, None)

      val form = dummyForm().bindFromRequest()(FakeRequest().withFormUrlEncodedBody(fuelWithdrawnBeforeCarBenefitStarted:_*))
      form.hasErrors shouldBe true
    }

    "reject a datefuelwithdrawn of samedate and a provided date" in new WithApplication {
      val fuelWithdrawnBeforeCarBenefitStarted = buildDateFormField(dateFuelWithdrawn, Some(("2014", "1", "1")))
      implicit val request = FakeRequest().withFormUrlEncodedBody(fuelWithdrawnBeforeCarBenefitStarted: _*)
      val form = dummyForm(CarFuelBenefitDates(Some(LocalDate(2013, 6, 7)), Some(FUEL_SAME_DATE))).bindFromRequest
      form.hasErrors shouldBe true
      form.errors(dateFuelWithdrawn).map(_.message) should contain("error.paye.remove_car_benefit.question4.extraDate")
    }
  }

  "validateFuelWithdrawnDate" should {

    val carBenefitStartDate = new LocalDate(currentTaxYear, 6, 30)

    case class DummyModel( dateFuelWithdrawn: LocalDate)

    def dummyForm(fuelDateWithdrawn : Option[LocalDate]) = {
      Form(
        mapping(
          withdrawDate -> localDateMapping(new LocalDate(), taxYearInterval, fuelDateWithdrawn, fuelBenefitMapping(Option(carBenefitStartDate)))
        )(DummyModel.apply)(DummyModel.unapply))
    }

    "reject the removal of car benefit when the car withdrawal date is before the fuel withdrawal date" in {
      val carWithdrawn = buildDateFormField(withdrawDate, Some(("2010", "6", "7")))

      val form = dummyForm(Some(new LocalDate(2010, 6, 8))).bindFromRequest()(FakeRequest().withFormUrlEncodedBody(carWithdrawn:_*))
      form.hasErrors shouldBe true
      form.errors(withdrawDate).map(err => err.message) should contain("error.paye.benefit.carwithdrawdate.before.fuelwithdrawdate")
    }

    "accept the removal of car benefit when the car withdrawal date is on the fuel withdrawal date" in {
      val carWithdrawn = buildDateFormField(withdrawDate, Some(("2010", "6", "8")))
      val form = dummyForm(Some(new LocalDate(2010, 6, 8))).bindFromRequest()(FakeRequest().withFormUrlEncodedBody(carWithdrawn:_*))
      form.errors(withdrawDate).map(err => err.message) should not contain "error.paye.benefit.carwithdrawdate.before.fuelwithdrawdate"
    }

    "accept the removal of car benefit when the car withdrawal after the fuel withdrawal date" in {
      val carWithdrawn = buildDateFormField(withdrawDate, Some(("2010", "6", "8")))
      val form = dummyForm(Some(new LocalDate(2010, 6, 7))).bindFromRequest()(FakeRequest().withFormUrlEncodedBody(carWithdrawn:_*))
      form.errors(withdrawDate).map(err => err.message) should not contain "error.paye.benefit.carwithdrawdate.before.fuelwithdrawdate"

    }
  }

  def bindFormWithValue[T](dummyForm: Form[T], field: String, value: String): Form[T] = {
    dummyForm.bindFromRequest()(FakeRequest().withFormUrlEncodedBody(field -> value))
  }

  def getValues( withdrawDateVal: Option[LocalDate] = None,
                 carUnavailableVal: Option[String] = None,
                 numberOfDaysUnavailableVal: Option[String] = None,
                 employeeContributesVal: Option[String] = None,
                 employeeContributionVal: Option[String] = None,
                 fuelDateChoiceVal: Option[String] = None,
                 fuelWithdrawDateVal: Option[LocalDate] = None) =
    RemoveCarBenefitFormDataValues(withdrawDateVal, carUnavailableVal, numberOfDaysUnavailableVal, employeeContributesVal, employeeContributionVal, fuelDateChoiceVal, fuelWithdrawDateVal)

}