package controllers.paye

import play.api.test.WithApplication
import models.paye.DisplayBenefit
import controllers.paye.RemovalUtils._
import org.joda.time.DateTime
import uk.gov.hmrc.utils.TaxYearResolver
import org.jsoup.Jsoup
import play.api.test.Helpers._
import models.paye.CarFuelBenefitDates
import play.api.test.FakeApplication
import scala.Some
import org.joda.time.chrono.ISOChronology

class ShowRemoveBenefitFormControllerSpec extends PayeBaseSpec {
  private lazy val dateToday: DateTime = new DateTime(2013, 12, 8, 12, 30, ISOChronology.getInstanceUTC)
  "Removing FUEL benefit only" should {

    import views.html.paye.remove_benefit_form

    "notify the user the fuel benefit will be removed for benefit with no company name" ignore new WithApplication(FakeApplication()) {

      val benefit = DisplayBenefit(johnDensmoresEmployments(0), johnDensmoresBenefits, None, Map.empty)
      val dates = Some(CarFuelBenefitDates(None, None))
      val form = updateBenefitForm(getStartDate(benefit.benefit), carBenefitWithUnremovedFuelBenefit = true, dates, dateToday)

      val result = remove_benefit_form(benefit, hasUnremovedCar = true, form, TaxYearResolver.currentTaxYearYearsRange)(johnDensmore)

      val doc = Jsoup.parse(contentAsString(result))

      doc.select(".benefit-type").text shouldBe "Your old company fuel benefit"
      doc.select("label[for=removeCar]").text should include("I would also like to remove my car benefit.")
    }
  }
}
