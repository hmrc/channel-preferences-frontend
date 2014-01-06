package controllers.paye

import models.paye._
import controllers.paye.RemovalUtils._
import views.html.paye.remove_car_benefit_form
import uk.gov.hmrc.utils.TaxYearResolver
import org.jsoup.Jsoup
import play.api.test.Helpers._
import org.joda.time.{LocalDate, DateTime}
import org.joda.time.chrono.ISOChronology
import uk.gov.hmrc.common.microservice.paye.domain.CarBenefit
import scala.Some
import play.api.test.{FakeRequest, FakeApplication, WithApplication}

class RemoveCarBenefitTemplateSpec extends PayeBaseSpec with MockedTaxYearSupport {
  private lazy val dateToday: DateTime = new DateTime(currentTaxYear, 12, 8, 12, 30, ISOChronology.getInstanceUTC)

  "the remove car benefit form" should {
    "not display the remove fuel benefit fields if the fuel benefit is present but already withdrawn" in new WithApplication(FakeApplication()) {

      val activeCarBenefit = CarBenefit(carBenefit, Some(withdrawnFuelBenefit))

      val form = updateRemoveCarBenefitForm(None, new LocalDate(), false, Some(CarFuelBenefitDates(None, None)), dateToday, taxYearInterval)

      val result = remove_car_benefit_form(activeCarBenefit, johnDensmoresOneEmployment(1).head, form, TaxYearResolver.currentTaxYearYearsRange)(johnDensmore, FakeRequest())

      val doc = Jsoup.parse(contentAsString(result))

      doc.select(".fuel-benefit-info").text shouldBe ""
    }
  }

}
