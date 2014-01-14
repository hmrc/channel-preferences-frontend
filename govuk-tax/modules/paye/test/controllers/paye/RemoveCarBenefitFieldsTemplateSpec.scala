package controllers.paye

import controllers.DateFieldsHelper
import play.api.test.{FakeRequest, FakeApplication, WithApplication}
import views.html.paye.remove_car_benefit_fields
import org.jsoup.Jsoup
import models.paye.{CarFuelBenefitDates, RemoveCarBenefitFormData}
import play.api.data.Form
import play.api.test.Helpers._
import controllers.paye.RemovalUtils._
import uk.gov.hmrc.utils.DateTimeUtils

class RemoveCarBenefitFieldsTemplateSpec extends PayeBaseSpec with DateFieldsHelper with StubTaxYearSupport {


  "remove car benefit template" should {

    "render the form with all the fields to remove a car benefit" in new WithApplication(FakeApplication()) {
      val form: Form[RemoveCarBenefitFormData] = updateRemoveCarBenefitForm(values = None,
        benefitStartDate = currentTestDate.toLocalDate,
        carBenefitWithUnremovedFuelBenefit = true,
        dates = Some(CarFuelBenefitDates(None, None)),
        DateTimeUtils.now,
        taxYearInterval)

      val currentTaxYearYearsRange = testTaxYear to testTaxYear + 1

      val result = remove_car_benefit_fields(form, currentTaxYearYearsRange, true)
      val doc = Jsoup.parse(contentAsString(result))

      doc.getElementById("withdrawDate.day") should bePresent
      doc.getElementById("withdrawDate.month") should bePresent
      doc.getElementById("withdrawDate.year") should bePresent
      doc.getElementById(s"withdrawDate.year-$testTaxYear") should bePresent
      doc.getElementById(s"withdrawDate.year-${testTaxYear + 1}") should bePresent

      doc.getElementById("carUnavailable-false") should bePresent
      doc.getElementById("carUnavailable-true") should bePresent

      doc.getElementById("numberOfDaysUnavailable") should bePresent

      doc.getElementById("removeEmployeeContributes-false") should bePresent
      doc.getElementById("removeEmployeeContributes-true") should bePresent

      doc.getElementById("removeEmployeeContribution") should bePresent

      doc.getElementById("fuelRadio-sameDateFuel") should bePresent
      doc.getElementById("fuelRadio-differentDateFuel") should bePresent

      doc.getElementById("fuelWithdrawDate.day") should bePresent
      doc.getElementById("fuelWithdrawDate.month") should bePresent
      doc.getElementById("fuelWithdrawDate.year") should bePresent
      doc.getElementById(s"fuelWithdrawDate.year-$testTaxYear") should bePresent
      doc.getElementById(s"fuelWithdrawDate.year-${testTaxYear + 1}") should bePresent
    }

    "render any errors in the form data" in new WithApplication(FakeApplication()) {
      val form: Form[RemoveCarBenefitFormData] = updateRemoveCarBenefitForm(values = None,
        benefitStartDate = currentTestDate.toLocalDate,
        carBenefitWithUnremovedFuelBenefit = true,
        dates = Some(CarFuelBenefitDates(None, None)),
        DateTimeUtils.now,
        taxYearInterval).bindFromRequest()(FakeRequest())
      val currentTaxYearYearsRange = testTaxYear to testTaxYear + 1

      val result = remove_car_benefit_fields(form, currentTaxYearYearsRange, true)
      val doc = Jsoup.parse(contentAsString(result))

      doc.getElementsByClass("form-field--error") should have size 4

    }

  }

}
