package controllers.paye.validation

import controllers.paye.{MockedTaxYearSupport, PayeBaseSpec}
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

class RemoveBenefitValidatorSpec  extends PayeBaseSpec with MockitoSugar with DateConverter with DateFieldsHelper with MockedTaxYearSupport {

  override def currentTaxYear = 2012
  val now = new LocalDate(currentTaxYear, 10, 2)
  val endOfTaxYear = new LocalDate(currentTaxYear, 4, 5)

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
}
