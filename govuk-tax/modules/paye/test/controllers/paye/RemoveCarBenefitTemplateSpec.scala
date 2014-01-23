package controllers.paye

import play.api.test.FakeApplication
import play.api.test.Helpers._
import play.api.test.{FakeRequest, WithApplication}

import org.jsoup.Jsoup
import org.joda.time.{LocalDate, DateTime}
import org.joda.time.chrono.ISOChronology

import uk.gov.hmrc.common.microservice.paye.domain.CarBenefit

import models.paye.CarFuelBenefitDates
import views.html.paye.remove_car_benefit_form
import views.html.paye.remove_fuel_benefit_form
import controllers.paye.RemovalUtils._

class RemoveCarBenefitTemplateSpec extends PayeBaseSpec with StubTaxYearSupport {
  private lazy val dateToday: DateTime = new DateTime(currentTaxYear, 12, 8, 12, 30, ISOChronology.getInstanceUTC)

  "Removing FUEL benefit only" should {
    "notify the user the fuel benefit will be removed for benefit with no company name" in new WithApplication(FakeApplication()) {

      val form = updateRemoveFuelBenefitForm(new LocalDate, dateToday, taxYearInterval)
      val fuelBenefit = CarBenefit(carBenefit, Some(withdrawnFuelBenefit)).fuelBenefit.get

      val result =
        remove_fuel_benefit_form(fuelBenefit, johnDensmoresOneEmployment(1).head, 2013, form, currentTaxYearYearsRange)(johnDensmore, requestWithCorrectVersion)

      val doc = Jsoup.parse(contentAsString(result))
      doc.select(".benefit-type").text shouldBe "Remove your fuel benefit"
    }
  }

  "the remove car benefit form" should {
    val form = updateRemoveCarBenefitForm(None, new LocalDate(), false, Some(CarFuelBenefitDates(None, None)), dateToday, taxYearInterval)
    val activeCarBenefit = CarBenefit(carBenefit, Some(withdrawnFuelBenefit))

    "not display the remove fuel benefit fields if the fuel benefit is present but already withdrawn" in new WithApplication(FakeApplication()) {
      val result = remove_car_benefit_form(activeCarBenefit, johnDensmoresOneEmployment(1).head, form, currentTaxYearYearsRange)(johnDensmore, FakeRequest())

      val doc = Jsoup.parse(contentAsString(result))

      doc.getElementById("fuelRadio-sameDateFuel") shouldBe null
      doc.getElementById("fuelRadio-differentDateFuel") shouldBe null
    }

  }


  "Given a user who has car and fuel benefits, removing car benefit " should {
    val activeCarBenefit = johnDensmoresBenefits.head
    val form = updateRemoveCarBenefitForm(None, new LocalDate(), false, Some(CarFuelBenefitDates(None, None)), dateToday, taxYearInterval)

    "In step 1, give the user the option to remove fuel benefit on the same (or different) date as the car" in new WithApplication(FakeApplication()) {
      val result = remove_car_benefit_form(activeCarBenefit, johnDensmoresOneEmployment(1).head, form, currentTaxYearYearsRange)(johnDensmore, FakeRequest())

      val doc = Jsoup.parse(contentAsString(result))
      doc.getElementById("fuelRadio-sameDateFuel") should not be null
      doc.getElementById("fuelRadio-differentDateFuel") should not be null
    }
  }

}
