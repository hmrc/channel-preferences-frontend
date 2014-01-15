package controllers.paye.validation

import controllers.paye.{StubTaxYearSupport, PayeBaseSpec}
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.utils.DateConverter
import controllers.DateFieldsHelper
import play.api.data.Form
import play.api.data.Forms._
import controllers.paye.CarBenefitFormFields._
import controllers.paye.validation.RemoveBenefitValidator._
import scala.Some
import play.api.test.{FakeRequest, FakeApplication, WithApplication}
import org.joda.time.LocalDate
import models.paye.CarFuelBenefitDates
import play.api.i18n.Messages

class RemoveBenefitValidatorSpec  extends PayeBaseSpec with MockitoSugar with DateConverter with DateFieldsHelper with StubTaxYearSupport {

  override def currentTaxYear = 2012
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

    "reject a value that is less than 0" in new WithApplication(FakeApplication()) {
      val form = bindFormWithValue(dummyForm(getValues(carUnavailableVal=Some("true"))), "daysUnavailable", "-1")
      form.hasErrors shouldBe true
      form.errors("daysUnavailable").map(err => Messages(err.message)) should contain ("Enter a number more than 0.")
    }

    "reject a value that is 0" in new WithApplication(FakeApplication()) {
      val form = bindFormWithValue(dummyForm(getValues(carUnavailableVal=Some("true"))), "daysUnavailable", "0")
      form.hasErrors shouldBe true
      form.errors("daysUnavailable").map(err => Messages(err.message)) should contain ("Enter a number more than 0.")
    }

    "accept a correct value" in new WithApplication(FakeApplication()) {
      val form = bindFormWithValue(dummyForm(getValues(carUnavailableVal=Some("true"))), "daysUnavailable", "32")
      form.hasErrors shouldBe false
    }

    "reject when the carUnavailable flag is true, but no value is provided." in new WithApplication(FakeApplication()) {
      val form = bindFormWithValue(dummyForm(getValues(carUnavailableVal=Some("true"))), "daysUnavailable", "")
      form.hasErrors shouldBe true
      form.errors("daysUnavailable").map(err => Messages(err.message)) should contain ("You must specify the number of consecutive days the car has been unavailable.")
    }

    "reject when the value is bigger than the providedFrom -> providedTo range." in new WithApplication(FakeApplication()) {
      val fromDate = new LocalDate(2012, 5, 30)
      val toDate = Some(new LocalDate(2012, 5, 31))
      val form = bindFormWithValue(dummyForm(getValues(carUnavailableVal=Some("true"), withdrawDateVal = toDate), fromDate), "daysUnavailable", "3")
      form.hasErrors shouldBe true
      form.errors("daysUnavailable").map(err => Messages(err.message)) should contain ("The car can’t be unavailable for longer than the total number of days you’ve had it. Reduce the number of days unavailable or check the date you got the car.")
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

    "reject a value that is more than 99999" in new WithApplication(FakeApplication()) {
      val form = bindFormWithValue(dummyForm(getValues(employeeContributesVal = Some("true"))), "employeeContribution", "100000")
      form.hasErrors shouldBe true
      form.errors("employeeContribution").map(err => Messages(err.message)) should contain ("Enter a number between £1 and £99,999.")
    }

    "reject a value that is less than 0" in new WithApplication(FakeApplication()) {
      val form = bindFormWithValue(dummyForm(getValues(employeeContributesVal=Some("true"))), "employeeContribution", "-1")
      form.hasErrors shouldBe true
      form.errors("employeeContribution").map(err => Messages(err.message)) should contain ("Employee payment must be greater than zero if you have selected yes.")
    }

    "reject a value that is 0" in new WithApplication(FakeApplication()) {
      val form = bindFormWithValue(dummyForm(getValues(employeeContributesVal=Some("true"))), "employeeContribution", "0")
      form.hasErrors shouldBe true
      form.errors("employeeContribution").map(err => Messages(err.message)) should contain ("Employee payment must be greater than zero if you have selected yes.")
    }

    "accept a correct value" in new WithApplication(FakeApplication()) {
      val form = bindFormWithValue(dummyForm(getValues(employeeContributesVal=Some("true"))), "employeeContribution", "3276")
      form.hasErrors shouldBe false
    }

    "reject when the employeeContributes flag is true, but no value is provided." in new WithApplication(FakeApplication()) {
      val form = bindFormWithValue(dummyForm(getValues(employeeContributesVal=Some("true"))), "employeeContribution", "")
      form.hasErrors shouldBe true
      form.errors("employeeContribution").map(err => Messages(err.message)) should contain ("You must specify how much you paid your employer for private use of the company car.")
    }

  }

  "validateFuelDate in this tax year with a different date for Fuel" should {

    val carBenefitStartDate = new LocalDate(currentTaxYear, 6, 30)
    val carBenefitEndDate = new LocalDate(currentTaxYear, 7, 30)
    val dates = new CarFuelBenefitDates(Option(carBenefitEndDate), Option(FUEL_DIFFERENT_DATE))

    case class DummyModel( dateFuelWithdrawn: Option[LocalDate])

    def dummyForm = {
      Form(
        mapping(
          dateFuelWithdrawn -> validateFuelDate(Option(dates), Option(carBenefitStartDate), taxYearInterval)
        )(DummyModel.apply)(DummyModel.unapply))
    }

    "accept a fuel withdrawn date equal to the car withdrawn date" in {
      val fuelDate = carBenefitEndDate
      val formWithFuelWithdrawn = buildDateFormField(dateFuelWithdrawn, Some((fuelDate.getYear.toString, fuelDate.getMonthOfYear.toString, fuelDate.getDayOfMonth.toString)))

      val form = dummyForm.bindFromRequest()(FakeRequest().withFormUrlEncodedBody(formWithFuelWithdrawn:_*))
      form.hasErrors shouldBe false
      form.value.get.dateFuelWithdrawn shouldBe Some(fuelDate)
    }

    "accept a fuel withdrawn date that is before the car withdrawn date" in {
      val fuelDate = carBenefitEndDate.minusDays(1)
      val formWithFuelWithdrawn = buildDateFormField(dateFuelWithdrawn, Some((fuelDate.getYear.toString, fuelDate.getMonthOfYear.toString, fuelDate.getDayOfMonth.toString)))

      val form = dummyForm.bindFromRequest()(FakeRequest().withFormUrlEncodedBody(formWithFuelWithdrawn:_*))
      form.hasErrors shouldBe false
      form.value.get.dateFuelWithdrawn shouldBe Some(fuelDate)
    }

    "reject a fuel withdrawn date that is after the car withdrawn date" in new WithApplication(FakeApplication()) {
      val fuelDate = carBenefitEndDate.plusDays(1)
      val fuelWithdrawnDateAfterCarWithdrawn = buildDateFormField(dateFuelWithdrawn, Some((fuelDate.getYear.toString, fuelDate.getMonthOfYear.toString, fuelDate.getDayOfMonth.toString)))

      val form = dummyForm.bindFromRequest()(FakeRequest().withFormUrlEncodedBody(fuelWithdrawnDateAfterCarWithdrawn:_*))
      form.hasErrors shouldBe true
    }

    "reject a fuel withdrawn date that is before the car benefit starting date" in new WithApplication(FakeApplication()) {
      val fuelDate = carBenefitStartDate.minusDays(1)
      val fuelWithdrawnBeforeCarBnefitStarted = buildDateFormField(dateFuelWithdrawn, Some((fuelDate.getYear.toString, fuelDate.getMonthOfYear.toString, fuelDate.getDayOfMonth.toString)))

      val form = dummyForm.bindFromRequest()(FakeRequest().withFormUrlEncodedBody(fuelWithdrawnBeforeCarBnefitStarted:_*))
      form.hasErrors shouldBe true
    }

    "reject a fuel withdrawn date that is empty" in new WithApplication(FakeApplication()) {
      val fuelWithdrawnBeforeCarBnefitStarted = buildDateFormField(dateFuelWithdrawn, None)

      val form = dummyForm.bindFromRequest()(FakeRequest().withFormUrlEncodedBody(fuelWithdrawnBeforeCarBnefitStarted:_*))
      form.hasErrors shouldBe true
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
