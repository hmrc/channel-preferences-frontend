package controllers.paye

import controllers.DateFieldsHelper
import play.api.test.{FakeRequest, FakeApplication, WithApplication}
import views.html.paye.add_car_benefit_fields
import org.jsoup.Jsoup
import models.paye.CarBenefitData
import play.api.data.Form
import play.api.test.Helpers._
import controllers.paye.validation.AddCarBenefitValidator._

class AddCarBenefitFieldsTemplateSpec extends PayeBaseSpec with DateFieldsHelper {


  "add car benefit template" should {

    "render the form with all the fields to add a car" in new WithApplication(FakeApplication()) {
      val form: Form[CarBenefitData] = carBenefitForm(CarBenefitValues())
      val employerName = Some("Sainsbury's")
      val currentTaxYearYearsRange = testTaxYear to testTaxYear + 1

      val result = add_car_benefit_fields(form, employerName, currentTaxYearYearsRange)
      val doc = Jsoup.parse(contentAsString(result))

      val bePresent = not be null

      doc.getElementById("providedFrom.day") should bePresent

      doc.getElementById("providedFrom.month") should bePresent
      doc.getElementById("providedFrom.year") should bePresent
      doc.getElementById(s"providedFrom.year-$testTaxYear") should bePresent
      doc.getElementById(s"providedFrom.year-${testTaxYear + 1}") should bePresent

      doc.getElementById("carRegistrationDate.day") should bePresent
      doc.getElementById("carRegistrationDate.month") should bePresent
      doc.getElementById("carRegistrationDate.year") should bePresent

      doc.getElementById("listPrice") should bePresent

      doc.getElementById("employeeContributes-false") should bePresent
      doc.getElementById("employeeContributes-true") should bePresent

      doc.getElementById("employeeContribution") should bePresent

      doc.getElementById("fuelType-diesel") should bePresent
      doc.getElementById("fuelType-electricity") should bePresent
      doc.getElementById("fuelType-other") should bePresent

      doc.getElementById("co2Figure") should bePresent
      doc.getElementById("co2NoFigure") should bePresent

      doc.getElementById("engineCapacity") should bePresent
      doc.getElementById("engineCapacity-1400") should bePresent
      doc.getElementById("engineCapacity-2000") should bePresent
      doc.getElementById("engineCapacity-9999") should bePresent

      doc.getElementById("employerContributes-false") should bePresent
      doc.getElementById("employerContributes-true") should bePresent

      doc.getElementById("employerContribution") should bePresent

      doc.getElementById("employerPayFuel-false") should bePresent
      doc.getElementById("employerPayFuel-true") should bePresent
    }

    "render any errors in the form data" in new WithApplication(FakeApplication()) {
      val form: Form[CarBenefitData] = carBenefitForm(CarBenefitValues()).bindFromRequest()(FakeRequest())
      val employerName = Some("Sainsbury's")
      val currentTaxYearYearsRange = testTaxYear to testTaxYear + 1

      val result = add_car_benefit_fields(form, employerName, currentTaxYearYearsRange)
      val doc = Jsoup.parse(contentAsString(result))

      doc.getElementsByClass("form-field--error") should have size 8

    }

  }

}
