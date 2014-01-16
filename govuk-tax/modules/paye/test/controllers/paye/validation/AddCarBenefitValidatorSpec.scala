package controllers.paye.validation

import controllers.paye.{StubTaxYearSupport, PayeBaseSpec}
import org.scalatest.mock.MockitoSugar
import play.api.data.{Mapping, Form}
import play.api.data.Forms._
import play.api.test.{WithApplication, FakeRequest}
import AddCarBenefitValidator._
import play.api.i18n.Messages
import controllers.paye.CarBenefitFormFields._
import org.joda.time.LocalDate
import uk.gov.hmrc.utils.DateConverter
import controllers.DateFieldsHelper
import controllers.paye.validation.AddCarBenefitValidator.CarBenefitValues
import play.api.test.FakeApplication

class AddCarBenefitValidatorSpec extends PayeBaseSpec with MockitoSugar with DateConverter with DateFieldsHelper with StubTaxYearSupport {
  override def currentTaxYear = 2013
   val now = new LocalDate(currentTaxYear, 10, 2)
   val endOfTaxYear = new LocalDate(currentTaxYear, 4, 5)

  "AddCarBenefitValidator for field ENGINE CAPACITY " should {

    case class DummyModel(engineCapacity: Option[String])

    def dummyForm = {
      Form(
        mapping(
          engineCapacity -> validateEngineCapacity(getValues())
        )(DummyModel.apply)(DummyModel.unapply))
    }

    "reject a value that is not one of the options" in new WithApplication(FakeApplication()) {
      val form = bindFormWithValue(dummyForm, engineCapacity, "-123")
      assertHasThisErrorMessage(form, engineCapacity, "Value not accepted. Please select one of the options.")
    }
    "accept a value that is one of the options" in {
      val form = bindFormWithValue(dummyForm, engineCapacity, "2000")
      form.hasErrors shouldBe false
      form.value.get.engineCapacity shouldBe Some("2000")
    }
  }


  "AddCarBenefitValidator for field NUMBER OF DAYS UNAVAILABLE" should {

    case class DummyModel(daysUnAvailable: Option[Int])

    val values = getValues(carUnavailableVal=Some("true"))

    def dummyForm(values:CarBenefitValues) = {
      Form(
        mapping(
          numberOfDaysUnavailable -> validateNumberOfDaysUnavailable(values, taxYearInterval)
        )(DummyModel.apply)(DummyModel.unapply))
    }

    "reject a value that is more than 999" in new WithApplication(FakeApplication()) {
      val form = bindFormWithValue(dummyForm(values), numberOfDaysUnavailable, "1000")
      assertHasThisErrorMessage(form, numberOfDaysUnavailable, "Enter a number between 1 and 999.")
    }

    "reject a value that is less than 0" in new WithApplication(FakeApplication()) {
      val form = bindFormWithValue(dummyForm(values), numberOfDaysUnavailable, "-1")
      assertHasThisErrorMessage(form, numberOfDaysUnavailable, "Enter a number more than 0.")
    }

    "reject a value that is 0" in new WithApplication(FakeApplication()) {
      val form = bindFormWithValue(dummyForm(values), numberOfDaysUnavailable, "0")
      assertHasThisErrorMessage(form, numberOfDaysUnavailable, "Enter a number more than 0.")
    }

    "accept a correct value" in new WithApplication(FakeApplication()) {
      val form = bindFormWithValue(dummyForm(values), numberOfDaysUnavailable, "32")
      form.hasErrors shouldBe false
    }

    "reject when the carUnavailable flag is true, but no value is provided." in new WithApplication(FakeApplication()) {
      val form = bindFormWithValue(dummyForm(values), numberOfDaysUnavailable, "")
      assertHasThisErrorMessage(form, numberOfDaysUnavailable, "Enter the number of days the car was unavailable.")
    }

    "reject when the value is bigger than the providedFrom -> providedTo range." in new WithApplication(FakeApplication()) {
      val fromDate = Some(new LocalDate(2012, 5, 30))
      val toDate = Some(new LocalDate(2012, 5, 31))
      val form = bindFormWithValue(dummyForm(getValues(providedFromVal = fromDate, providedToVal = toDate, carUnavailableVal=Some("true"), giveBackThisTaxYearVal = Some("true"))), numberOfDaysUnavailable, "3")
      assertHasThisErrorMessage(form, numberOfDaysUnavailable, "The car can’t be unavailable for longer than the total number of days you’ve had it. Reduce the number of days unavailable or check the date you got the car.")
    }
  }

  "AddCarBenefitValidator for fields EMPLOYER PAY FUEL & DATE FUEL WITHDRAWN" should {

    case class DummyModel(employerPayFuel: Option[String], dateFuelWithdrawn: Option[LocalDate])

    val values = getValues()
    def dummyForm(values: CarBenefitValues) = {
      Form(
        mapping(
          employerPayFuel -> validateEmployerPayFuel(values),
          dateFuelWithdrawn -> validateDateFuelWithdrawn(values, taxYearInterval)
        )(DummyModel.apply)(DummyModel.unapply))
    }

    "reject a value that is not one of the options for the employer pay fuel field" in new WithApplication(FakeApplication()) {
      val form = bindFormWithValue(dummyForm(values), employerPayFuel, "-123")
      assertHasThisErrorMessage(form, employerPayFuel, "Value not accepted. Please select one of the options.")
    }

    "accept a value for fuel date withdrawn if providedTo is not empty but giveBackCarThisYear is false" in new WithApplication(FakeApplication()) {
      val withdrawnDateAfterCarProvidedTo = buildDateFormField(dateFuelWithdrawn, Some((s"$currentTaxYear", "6", "7")))
      val form = dummyForm(getValues(
        employerPayFuelVal = Some("date"),
        giveBackThisTaxYearVal = Some("false"),
        providedToVal = Some(new LocalDate(currentTaxYear,5,5)))
      ).bindFromRequest()(FakeRequest().withFormUrlEncodedBody(withdrawnDateAfterCarProvidedTo: _*))

      form.errors(dateFuelWithdrawn) shouldBe empty
    }

    "reject if there is no value for the employer pay fuel field" in new WithApplication(FakeApplication()) {
      val form = bindFormWithValue(dummyForm(values), employerPayFuel, "")
      assertHasThisErrorMessage(form, employerPayFuel, "Please answer this question.")
    }

    "reject if date fuel withdrawn is not a valid date" in new WithApplication(FakeApplication()) {
      val wrongDate = buildDateFormField(dateFuelWithdrawn, Some(("a", "f", "")))
      val form = dummyForm(getValues(employerPayFuelVal = Some("date"))).bindFromRequest()(FakeRequest().withFormUrlEncodedBody(wrongDate: _*))
      assertHasThisErrorMessage(form, dateFuelWithdrawn, "You must specify a valid date")
    }

    "reject if date fuel withdrawn is incomplete" in new WithApplication(FakeApplication()) {
      val wrongDate = buildDateFormField(dateFuelWithdrawn, Some((currentTaxYear.toString, "6", "f")))
      val form = dummyForm(getValues(employerPayFuelVal = Some("date"))).bindFromRequest()(FakeRequest().withFormUrlEncodedBody(wrongDate: _*))
      assertHasThisErrorMessage(form, dateFuelWithdrawn, "You must specify a valid date")
    }

    "reject fuel withdrawn if it is formed of numbers but not a valid date" in new WithApplication(FakeApplication()) {
      val wrongDate = buildDateFormField(dateFuelWithdrawn, Some((currentTaxYear.toString, "2", "31")))
      val form = dummyForm(getValues(employerPayFuelVal = Some("date"))).bindFromRequest()(FakeRequest().withFormUrlEncodedBody(wrongDate: _*))
      assertHasThisErrorMessage(form, dateFuelWithdrawn, "You must specify a valid date")
    }

    "reject fuel withdrawn date if it is before the car benefit was made available" in new WithApplication(FakeApplication()) {
      val fuelWithdrawn = buildDateFormField(dateFuelWithdrawn, Some((now.getYear.toString, "6", "11")))
      val carStartDate = Some(new LocalDate(currentTaxYear, 6, 12))
      val carStopDate = Some(new LocalDate(currentTaxYear, 8 , 4))

      val valuesWithCarProvidedDates = getValues(
        employerPayFuelVal = Some("date"),
        providedFromVal = carStartDate,
        providedToVal = carStopDate,
        giveBackThisTaxYearVal = Some("true"))
      val form = dummyForm(valuesWithCarProvidedDates).bindFromRequest()(FakeRequest().withFormUrlEncodedBody(fuelWithdrawn: _*))
      assertHasThisErrorMessage(form, dateFuelWithdrawn, "Enter a date that’s on or after the date you got the car.")
    }

    "reject fuel withdrawn date if it is after the date when the car benefit was removed"  in new WithApplication(FakeApplication()) {
      val fuelWithdrawn = buildDateFormField(dateFuelWithdrawn, Some((currentTaxYear.toString, "8", "5")))
      val carStartDate = Some(new LocalDate(currentTaxYear, 6, 12))
      val carStopDate = Some(new LocalDate(currentTaxYear, 8 , 4))

      val valuesWithCarProvidedDates = getValues(
        employerPayFuelVal = Some("date"),
        providedFromVal = carStartDate,
        providedToVal = carStopDate,
        giveBackThisTaxYearVal = Some("true"))
      val form = dummyForm(valuesWithCarProvidedDates).bindFromRequest()(FakeRequest().withFormUrlEncodedBody(fuelWithdrawn: _*))

      assertHasThisErrorMessage(form, dateFuelWithdrawn, "Enter a date that’s on or before the date you returned the car.")
    }

    "accept fuel withdrawn date if it is the same date than when the car benefit was removed"  in new WithApplication(FakeApplication()) {
      val fuelWithdrawn = buildDateFormField(dateFuelWithdrawn, Some((currentTaxYear.toString, "8", "4")))
      val carStartDate = Some(new LocalDate(currentTaxYear, 6, 12))
      val carStopDate = Some(new LocalDate(currentTaxYear, 8 , 4))

      val valuesWithCarProvidedDates = getValues(employerPayFuelVal = Some("date"), providedFromVal = carStartDate, providedToVal = carStopDate)
      val form = dummyForm(valuesWithCarProvidedDates).bindFromRequest()(FakeRequest().withFormUrlEncodedBody(fuelWithdrawn: _*))

      form.errors(dateFuelWithdrawn).size shouldBe 0
    }

    "reject fuel withdrawn date if it is the same date the car benefit is provided from"  in new WithApplication(FakeApplication()) {
      val fuelWithdrawn = buildDateFormField(dateFuelWithdrawn, Some((currentTaxYear.toString, "6", "12")))
      val carStartDate = Some(new LocalDate(currentTaxYear, 6, 12))
      val carStopDate = Some(new LocalDate(currentTaxYear, 8 , 4))

      val valuesWithCarProvidedDates = getValues(employerPayFuelVal = Some("date"), providedFromVal = carStartDate, providedToVal = carStopDate)
      val form = dummyForm(valuesWithCarProvidedDates).bindFromRequest()(FakeRequest().withFormUrlEncodedBody(fuelWithdrawn: _*))

      assertHasThisErrorMessage(form, dateFuelWithdrawn, "Enter a date that’s on or after the date you got the car.")
    }

    "reject fuel benefit if fuel type is electricity" in new WithApplication(FakeApplication()) {
      def haveErrors = have ('hasErrors (true))
      val form = dummyForm(getValues(fuelTypeVal = Some("electricity"), employerPayFuelVal = Some("Yes"))).bindFromRequest()(FakeRequest().withFormUrlEncodedBody(fuelType -> "electricity", employerPayFuel -> "true"))
      assertHasThisErrorMessage(form, employerPayFuel, "You can’t claim fuel benefit for electric cars or cars with zero C02 emissions.")
    }
  }

  "AddCarBenefitValidator for field CO2 FIGURE & CO2 NO FIGURE " should {

    case class Co2DummyModel(co2FigureField: Option[Int], co2NoFigure: Option[Boolean])

    val values = getValues(fuelTypeVal = Some("diesel"))

    def dummyForm(values: CarBenefitValues) = {
      Form[Co2DummyModel](
        mapping(
          co2Figure -> validateCo2Figure(values),
          co2NoFigure -> validateNoCo2Figure(values)
        )(Co2DummyModel.apply)(Co2DummyModel.unapply))
    }

    "reject a negative integer" in new WithApplication(FakeApplication()) {
      val form = bindFormWithValue(dummyForm(values), co2Figure, "-123")
      assertHasThisErrorMessage(form, co2Figure, "Enter a number between 1 and 999.")
    }

    "reject an integer with more than 3 characters" in new WithApplication(FakeApplication()) {
      val form = bindFormWithValue(dummyForm(values), co2Figure, "1000")
      assertHasThisErrorMessage(form, co2Figure, "Enter a number between 1 and 999.")
    }

    "reject a number value which is not an integer" in new WithApplication(FakeApplication()) {
      val form = bindFormWithValue(dummyForm(values), co2Figure, "37.3")
      form.hasErrors shouldBe true
      assertHasThisErrorMessage(form, co2Figure, "Please use whole numbers only, not decimals or other characters.")
    }

    "reject a value which is not an integer" in new WithApplication(FakeApplication()) {
      val form = bindFormWithValue(dummyForm(values), co2Figure, "dmdknadsfklads.(0383k378@__//")
      assertHasThisErrorMessage(form, co2Figure, "Please use whole numbers only, not decimals or other characters.")
    }

    "accept a valid integer" in {
      val form = dummyForm(getValues(co2FigureVal = Some("342"))).bindFromRequest()(FakeRequest().withFormUrlEncodedBody(co2Figure -> "342"))
      form.hasErrors shouldBe false
      form.value.get.co2FigureField shouldBe Some(342)
    }

    "reject a NO CO2 FIGURE that is not a boolean" in new WithApplication(FakeApplication()) {
      val form = bindFormWithValue(dummyForm(values), co2NoFigure, "dmdknadsfklads.(0383k378@__//")
      form.hasErrors shouldBe true
    }

    "accept a NO CO2 FIGURE that is a boolean" in new WithApplication(FakeApplication()) {
      val form = bindFormWithValue(dummyForm(values), co2NoFigure, "true")
      form.hasErrors shouldBe false
    }

    "accept a NO CO2 FIGURE that is a boolean and an empty CO2 figure" in new WithApplication(FakeApplication()) {
      val form = dummyForm(getValues(co2NoFigureVal = Some("true"))).bindFromRequest()(FakeRequest().withFormUrlEncodedBody(co2Figure -> "", co2NoFigure -> "true"))
      form.hasErrors shouldBe false
    }
  }

  "AddCarBenefitValidator for fields CAR REGISTRATION DATE, FUEL TYPE, CO2 FIGUREs and ENGINE CAPACITY  " should {

    case class FiguresDummyModel(carRegistrationDate: Option[LocalDate], fuelType: Option[String], co2Figure: Option[Int], co2NoFigure: Option[Boolean], engineCapacity: Option[String])

    def dummyForm(values: CarBenefitValues) = {
      Form[FiguresDummyModel](
        mapping(
          carRegistrationDate -> validateCarRegistrationDate(() => now),
          fuelType -> validateFuelType(values),
          co2Figure -> validateCo2Figure(values),
          co2NoFigure -> validateNoCo2Figure(values),
          engineCapacity -> validateEngineCapacity(values)
        )(FiguresDummyModel.apply)(FiguresDummyModel.unapply))
    }

    "accept the car registration date if it in 1900" in new WithApplication(FakeApplication()) {
      val goodRegistrationDate = buildDateFormField(carRegistrationDate, Some(("1900", "1", "1")))

      val form = dummyForm(getValues()).bindFromRequest()(FakeRequest().withFormUrlEncodedBody(goodRegistrationDate:_*))
      form.errors(carRegistrationDate).size shouldBe 0
    }

    "reject the car registration date if it later than today" in new WithApplication(FakeApplication()) {
      val tooLateDate = now.plusDays(1)
      val tooLateRegistrationDate = buildDateFormField(carRegistrationDate, Some((tooLateDate.getYear.toString, tooLateDate.getMonthOfYear.toString, tooLateDate.getDayOfMonth.toString)))

      val form = dummyForm(getValues()).bindFromRequest()(FakeRequest().withFormUrlEncodedBody(tooLateRegistrationDate:_*))
      form.errors(carRegistrationDate) should have size 1
      assertHasThisErrorMessage(form, carRegistrationDate, "Enter a date that’s on or before today.")
    }

    "accept the car registration date if it is today" in new WithApplication(FakeApplication()) {
      val tooLateDate = now
      val tooLateRegistrationDate = buildDateFormField(carRegistrationDate, Some((tooLateDate.getYear.toString, tooLateDate.getMonthOfYear.toString, tooLateDate.getDayOfMonth.toString)))

      val form = dummyForm(getValues()).bindFromRequest()(FakeRequest().withFormUrlEncodedBody(tooLateRegistrationDate:_*))
      form.errors(carRegistrationDate) should have size 0
    }

    "reject registered before 98 if it is blank" in new WithApplication(FakeApplication()) {
      val form = dummyForm(getValues()).bindFromRequest()(FakeRequest().withFormUrlEncodedBody(carRegistrationDate -> ""))
      assertHasThisErrorMessage(form, carRegistrationDate, "Please answer this question.")
    }

    "reject car registered date if it is before 1900" in new WithApplication(FakeApplication()) {
      val registartionDateBefore1900 = buildDateFormField(carRegistrationDate, Some(("1899", "12", "31")))
      val form = dummyForm(getValues()).bindFromRequest()(FakeRequest().withFormUrlEncodedBody(registartionDateBefore1900:_*))
      form.errors(carRegistrationDate).size shouldBe 1
      assertHasThisErrorMessage(form, carRegistrationDate, "Enter a year after 1900.")
    }

    "reject fuel type is electricity if registered before 98 is true" in new WithApplication(FakeApplication()) {
      val form = dummyForm(getValues(carRegistrationDateVal = Some(now.withYear(1996)), fuelTypeVal = Some("electricity"))).bindFromRequest()(FakeRequest().withFormUrlEncodedBody(carRegistrationDate -> "true", fuelType -> "electricity"))
      assertHasThisErrorMessage(form, fuelType, "If the car was registered with DVLA before 1998 the fuel can’t be electric and the emissions figure must be more than \"0\".")
    }

    "accept fuel type is electricity if registered the first of january 98" in new WithApplication(FakeApplication()) {
      val form = dummyForm(getValues(carRegistrationDateVal = Some(new LocalDate(1998, 1, 1)), fuelTypeVal = Some("electricity"))).bindFromRequest()(FakeRequest().withFormUrlEncodedBody(carRegistrationDate -> "true", fuelType -> "electricity"))
      form.errors(fuelType).size shouldBe 0
    }

    "reject co2 figure value if fuel type is electricity and co2 figure is not blank" in new WithApplication(FakeApplication()) {
      val form = dummyForm(getValues(fuelTypeVal = Some("electricity"))).bindFromRequest()(FakeRequest().withFormUrlEncodedBody(fuelType -> "electricity", co2Figure -> "123"))
      assertHasThisErrorMessage(form, co2Figure, "Don’t enter an emissions figure if the car fuel is electric or the emission figure is \"0\".")
    }

    "reject co2 no figure value if fuel type is electricity and no co2 figure is true" in new WithApplication(FakeApplication()) {
      val form = dummyForm(getValues(fuelTypeVal = Some("electricity"))).bindFromRequest()(FakeRequest().withFormUrlEncodedBody(fuelType -> "electricity", co2NoFigure -> "true"))
      assertHasThisErrorMessage(form, co2NoFigure, "Don’t enter an emissions figure if the car fuel is electric or the emission figure is \"0\".")
    }

    "reject CO2 figure if Co2 no figure is selected" in new WithApplication(FakeApplication()) {
      val form = dummyForm(getValues(co2NoFigureVal = Some("true"))).bindFromRequest()(FakeRequest().withFormUrlEncodedBody(co2Figure -> "123", co2NoFigure -> "true"))
      assertHasThisErrorMessage(form, co2Figure, "Enter an emissions figure or confirm that one is unavailable.")
    }

    "reject CO2 figures with only one error message for blank field if fuel type is electricity" in new WithApplication(FakeApplication()) {
      val form = dummyForm(getValues(fuelTypeVal = Some("electricity"),  co2FigureVal = Some("123") ,  co2NoFigureVal = Some("true"))).bindFromRequest()(FakeRequest().withFormUrlEncodedBody(fuelType -> "electricity", co2Figure -> "123", co2NoFigure -> "true"))
      assertHasThisErrorMessage(form, co2Figure, "Don’t enter an emissions figure if the car fuel is electric or the emission figure is \"0\".")
      form.errors(co2NoFigure) shouldBe empty
    }

    "reject engine capacity value if fuel type is electricity and engine capacity is not blank" in new WithApplication(FakeApplication()) {
      val form = dummyForm(getValues(fuelTypeVal = Some("electricity"))).bindFromRequest()(FakeRequest().withFormUrlEncodedBody(fuelType -> "electricity", engineCapacity -> "1400"))
      assertHasThisErrorMessage(form, engineCapacity, "Don’t select an engine size if the car fuel is electric or the emission figure is \"0\".")
    }

    "accept engine capacity value if fuel type is electricity and engine capacity is blank" in new WithApplication(FakeApplication()) {
      val form = dummyForm(getValues(fuelTypeVal = Some("electricity"))).bindFromRequest()(FakeRequest().withFormUrlEncodedBody(fuelType -> "electricity"))
      form.errors(engineCapacity) shouldBe empty
    }

    "reject co2 figures if co2 no figure is false (blank) and co2 figure is blank and fuel type is not electricity" in new WithApplication(FakeApplication()) {
      val form = dummyForm(getValues(fuelTypeVal = Some("diesel"))).bindFromRequest()(FakeRequest().withFormUrlEncodedBody(fuelType -> "diesel", co2Figure -> "", co2NoFigure -> ""))
      assertHasThisErrorMessage(form, co2NoFigure, "Confirm the car’s CO2 emissions.")
    }

    "reject engine capacity blank if fuel type is not electricity" in new WithApplication(FakeApplication()) {
      val form = dummyForm(getValues(carRegistrationDateVal = Some(now.withYear(1996)), fuelTypeVal = Some("diesel"))).bindFromRequest()(FakeRequest().withFormUrlEncodedBody(carRegistrationDate -> "true"))
      assertHasThisErrorMessage(form, engineCapacity, "Select an engine size if the car’s emission figure is more than zero or the fuel type is diesel, petrol, hybrid or other.")
    }
  }

  def bindFormWithValue[T](dummyForm: Form[T], field: String, value: String): Form[T] = {
    dummyForm.bindFromRequest()(FakeRequest().withFormUrlEncodedBody(field -> value))
  }

  def assertHasThisErrorMessage[T](form: Form[T], field: String, expectedErrorMessage: String) = {
    form.hasErrors shouldBe true
    form.errors(field).map(err => Messages(err.message)) should contain (expectedErrorMessage)
  }

  def getValues(fuelTypeVal: Option[String] = None, co2NoFigureVal: Option[String] = None, co2FigureVal: Option[String] = None,
                carRegistrationDateVal: Option[LocalDate] = None, numberOfDaysUnavailableVal: Option[String] = None, employerPayFuelVal: Option[String] = None,
                providedFromVal: Option[LocalDate] = None, providedToVal: Option[LocalDate] = None, carUnavailableVal: Option[String] = None,
                giveBackThisTaxYearVal: Option[String] = None) =
    new CarBenefitValues(
        providedFromVal = providedFromVal,
        carUnavailableVal = carUnavailableVal,
        numberOfDaysUnavailableVal = numberOfDaysUnavailableVal,
        giveBackThisTaxYearVal = giveBackThisTaxYearVal,
        providedToVal = providedToVal,
        carRegistrationDate = carRegistrationDateVal,
        employeeContributes = None,
        privateUsePayment = None,
        fuelType = fuelTypeVal,
        co2Figure = co2FigureVal,
        co2NoFigure = co2NoFigureVal,
        employerPayFuel = employerPayFuelVal)
}
