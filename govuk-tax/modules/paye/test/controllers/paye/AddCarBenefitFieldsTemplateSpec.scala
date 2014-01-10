package controllers.paye

import controllers.DateFieldsHelper
import play.api.test.{FakeRequest, FakeApplication, WithApplication}
import views.html.paye.add_car_benefit_fields
import org.jsoup.Jsoup
import models.paye.CarBenefitData
import play.api.data.Form
import play.api.test.Helpers._
import controllers.paye.validation.AddCarBenefitValidator._
import org.joda.time.LocalDate

class AddCarBenefitFieldsTemplateSpec extends PayeBaseSpec with DateFieldsHelper {
  "add car benefit template" should {
    "render the form with all the fields to add a car" in new WithApplication(FakeApplication()) {
      val form: Form[CarBenefitData] = carBenefitForm(CarBenefitValues())
      val employerName = Some("Sainsbury's")
      val currentTaxYearYearsRange = testTaxYear to testTaxYear + 1

      val result = add_car_benefit_fields(form, employerName, currentTaxYearYearsRange)
      val doc = Jsoup.parse(contentAsString(result))

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

      doc.getElementById("engineCapacity") should not(bePresent)
      doc.getElementById("engineCapacity-1400") should bePresent
      doc.getElementById("engineCapacity-2000") should bePresent
      doc.getElementById("engineCapacity-9999") should bePresent

      doc.getElementById("employerContributes-false") should bePresent
      doc.getElementById("employerContributes-true") should bePresent

      doc.getElementById("employerContribution") should bePresent

      doc.getElementById("employerPayFuel-false") should bePresent
      doc.getElementById("employerPayFuel-true") should bePresent
    }


    "render the form with a bunch of prepoulated data" in new WithApplication(FakeApplication()) {
      val form: Form[CarBenefitData] = carBenefitForm(CarBenefitValues(employeeContributes = Some("true"), employerContributes = Some("true"))).fill(johnDensmoresCarBenefitData)
      val employerName = Some("Sainsbury's")
      val currentTaxYearYearsRange = testTaxYear to testTaxYear + 1

      val result = add_car_benefit_fields(form, employerName, currentTaxYearYearsRange)
      val doc = Jsoup.parse(contentAsString(result))

      doc.select("[id~=providedFrom]").select("[id~=day-29]").attr("selected") shouldBe "selected"
      doc.select("[id~=providedFrom]").select("[id~=month-7]").attr("selected") shouldBe "selected"
      doc.select("[id~=providedFrom]").select(s"[id~=year-$testTaxYear]").attr("selected") shouldBe "selected"
      doc.select("#listPrice").attr("value") shouldBe "1000"
      doc.select("#employerContributes-true").attr("checked") shouldBe "checked"
      doc.select("#employerContribution").attr("value") shouldBe "999"

      doc.select("[id~=carRegistrationDate]").select("[id~=day-13]").attr("selected") shouldBe "selected"
      doc.select("[id~=carRegistrationDate]").select("[id~=month-9]").attr("selected") shouldBe "selected"
      doc.select("[id~=carRegistrationDate]").select("[id~=year]").attr("value") shouldBe "1950"
      doc.select("#fuelType-electricity").attr("checked") shouldBe "checked"
      doc.select("#engineCapacity-2000").attr("checked") shouldBe "checked"
      doc.select("#employerPayFuel-false").attr("checked") shouldBe "checked"
      doc.select("#employeeContributes-true").attr("checked") shouldBe "checked"
      doc.select("#employeeContribution").attr("value") shouldBe "100"

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
