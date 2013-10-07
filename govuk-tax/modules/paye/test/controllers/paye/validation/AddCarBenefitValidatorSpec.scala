package controllers.paye.validation

import controllers.paye.PayeBaseSpec
import org.scalatest.mock.MockitoSugar
import play.api.data.Form
import play.api.data.Forms._
import play.api.test.{FakeApplication, WithApplication, FakeRequest}
import AddCarBenefitValidator._
import play.api.i18n.Messages
import controllers.paye.CarBenefitFormFields._
import org.joda.time.LocalDate
import uk.gov.hmrc.utils.{TaxYearResolver, DateConverter}
import controllers.DateFieldsHelper

class AddCarBenefitValidatorSpec extends PayeBaseSpec with MockitoSugar  with DateConverter with DateFieldsHelper {


  "AddCarBenefitValidator for field ENGINE CAPACITY " should {

    case class DummyModel(engineCapacity: Option[String])

    def dummyForm = {
      Form(
        mapping(
          engineCapacity -> validateEngineCapacity
        )(DummyModel.apply)(DummyModel.unapply))
    }

    "reject a value that is not one of the options" in new WithApplication(FakeApplication()) {
      val form = bindFormWithValue(dummyForm, engineCapacity, "-123")
      form.hasErrors shouldBe true
      assertHasThisErrorMessage(form, engineCapacity, "Value not accepted. Please select one of the options.")
    }
    "accept a value that is one of the options" in {
      val form = bindFormWithValue(dummyForm, engineCapacity, "2000")
      form.hasErrors shouldBe false
      form.value.get.engineCapacity shouldBe Some("2000")
    }
  }

  "AddCarBenefitValidator for fields EMPLOYER PAY FUEL & DATE FUEL WITHDRAWN" should {

    case class DummyModel(employerPayFuel: String, dateFuelWithdrawn: Option[LocalDate])

    def dummyForm = {
      Form(
        mapping(
          employerPayFuel -> validateEmployerPayFuel,
          dateFuelWithdrawn -> validateDateFuelWithdrawn
        )(DummyModel.apply)(DummyModel.unapply))
    }

    "reject a value that is not one of the options for the employer pay fuel field" in new WithApplication(FakeApplication()) {
      val form = bindFormWithValue(dummyForm, employerPayFuel, "-123")
      form.hasErrors shouldBe true
      assertHasThisErrorMessage(form, employerPayFuel, "Value not accepted. Please select one of the options.")
    }

    "reject if there is no value for the employer pay fuel field" in new WithApplication(FakeApplication()) {
      val form = bindFormWithValue(dummyForm, employerPayFuel, "")
      form.hasErrors shouldBe true
      assertHasThisErrorMessage(form, employerPayFuel, "This field is required")
    }

    "accept a value that is one of the options" in {
      val form = bindFormWithValue(dummyForm, employerPayFuel, "again")
      form.hasErrors shouldBe false
      form.value.get.employerPayFuel shouldBe "again"
    }

    "reject if date fuel withdrawn is not a valid date" in new WithApplication(FakeApplication()){
      val wrongDate = buildDateFormField(dateFuelWithdrawn, Some("a", "f", ""))
      val form = dummyForm.bindFromRequest()(FakeRequest().withFormUrlEncodedBody(wrongDate: _*))
      form.hasErrors shouldBe true
      assertHasThisErrorMessage(form, dateFuelWithdrawn, "You must specify a valid date")
    }

    "reject fuel withdrawn if it is formed of numbers but not a valid date" in new WithApplication(FakeApplication()){
      val wrongDate = buildDateFormField(dateFuelWithdrawn, Some("31", "2", TaxYearResolver.currentTaxYear.toString))
      val form = dummyForm.bindFromRequest()(FakeRequest().withFormUrlEncodedBody(wrongDate: _*))
      form.hasErrors shouldBe true
      assertHasThisErrorMessage(form, dateFuelWithdrawn, "You must specify a valid date")
    }

    "accept if date fuel withdrawn is a valid date" in new WithApplication(FakeApplication()){
      val paramsWithDate = buildDateFormField(dateFuelWithdrawn, Some(localDateToTuple(Some(new LocalDate())))) ++ Seq(employerPayFuel -> "again")
      val form = dummyForm.bindFromRequest()(FakeRequest().withFormUrlEncodedBody(paramsWithDate: _*))
      form.hasErrors shouldBe false
    }

  }

  "AddCarBenefitValidator for field CO2 FIGURE & CO2 NO FIGURE " should {

    case class DummyModel(co2FigureField: Option[Int], co2NoFigure: Option[Boolean])

    def dummyForm = {
      Form[DummyModel](
        mapping(
          co2Figure -> validateCo2Figure,
          co2NoFigure -> validateNoCo2Figure
        )(DummyModel.apply)(DummyModel.unapply))
    }

    "reject a negative integer" in new WithApplication(FakeApplication()) {
      val form = bindFormWithValue(dummyForm, co2Figure, "-123")
      form.hasErrors shouldBe true
      assertHasThisErrorMessage(form, co2Figure, "You must provide a number greater than zero.")

    }

    "reject an integer with more than 3 characters" in new WithApplication(FakeApplication()) {
      val form = bindFormWithValue(dummyForm, co2Figure, "1000")
      form.hasErrors shouldBe true
      assertHasThisErrorMessage(form, co2Figure, "You must provide a number which is 3 characters or less.")
    }

    "reject a number value which is not an integer" in new WithApplication(FakeApplication()) {
      val form = bindFormWithValue(dummyForm, co2Figure, "37.3")
      form.hasErrors shouldBe true
      assertHasThisErrorMessage(form, co2Figure, "Please use whole numbers only, not decimals or other characters.")
    }

    "reject a value which is not an integer" in new WithApplication(FakeApplication()) {
      val form = bindFormWithValue(dummyForm, co2Figure, "dmdknadsfklads.(0383k378@__//")
      form.hasErrors shouldBe true
      assertHasThisErrorMessage(form, co2Figure, "Please use whole numbers only, not decimals or other characters.")
    }

    "accept a valid integer" in {
      val form = bindFormWithValue(dummyForm, co2Figure, "342")
      form.hasErrors shouldBe false
      form.value.get.co2FigureField shouldBe Some(342)
    }

    "reject a NO CO2 FIGURE that is not a boolean" in new WithApplication(FakeApplication()) {
      val form = bindFormWithValue(dummyForm, co2NoFigure, "dmdknadsfklads.(0383k378@__//")
      form.hasErrors shouldBe true
    }

  }


  def bindFormWithValue[T](dummyForm: Form[T], field: String, value: String): Form[T] = {
    dummyForm.bindFromRequest()(FakeRequest().withFormUrlEncodedBody(field -> value))
  }

  def assertHasThisErrorMessage[T](form: Form[T], field: String, expectedErrorMessage: String) = {
    Messages(form.errors(field).head.message) shouldBe expectedErrorMessage
  }
}
