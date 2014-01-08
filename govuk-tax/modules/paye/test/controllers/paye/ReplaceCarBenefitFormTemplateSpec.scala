package controllers.paye

import controllers.DateFieldsHelper
import play.api.test.{FakeRequest, WithApplication}
import views.html.paye.{replace_car_benefit_form, remove_car_benefit_fields}
import org.jsoup.Jsoup
import models.paye.RemoveCarBenefitFormData
import play.api.data.Form
import play.api.test.Helpers._
import controllers.paye.RemovalUtils._
import uk.gov.hmrc.utils.DateTimeUtils
import controllers.paye.validation.AddCarBenefitValidator._
import models.paye.CarBenefitData
import models.paye.CarFuelBenefitDates
import play.api.test.FakeApplication
import scala.Some
import uk.gov.hmrc.common.microservice.paye.domain.CarBenefit
import org.scalatest.LoneElement
import scala.collection.JavaConversions
import JavaConversions._
import play.api.i18n.Messages


class ReplaceCarBenefitFormTemplateSpec extends PayeBaseSpec  with MockedTaxYearSupport with LoneElement {


  "replace car benefit form template" should {

    implicit val user = johnDensmore

    "render the form with all the fields to replace a car benefit" in new WithApplication(FakeApplication()) {
      val addForm: Form[CarBenefitData] = carBenefitForm(CarBenefitValues())
      val updateForm: Form[RemoveCarBenefitFormData] = updateRemoveCarBenefitForm(values = None,
        benefitStartDate = currentTestDate.toLocalDate,
        carBenefitWithUnremovedFuelBenefit = true,
        dates = Some(CarFuelBenefitDates(None, None)),
        DateTimeUtils.now,
        taxYearInterval)

      val activeCarBenefit = CarBenefit(carBenefit)
      val employment = johnDensmoresOneEmployment().loneElement
      val currentTaxYearYearsRange = testTaxYear to testTaxYear + 1

      val result = replace_car_benefit_form(activeCarBenefit, employment, updateForm, addForm, currentTaxYearYearsRange)
      val doc = Jsoup.parse(contentAsString(result))

      val benefitType = doc.getElementsByClass("benefit-type").toList.map(_.text)
      benefitType should have size 2
      benefitType should contain (Messages("paye.replace_car_benefit.benefit_type"))
      benefitType should contain (Messages("paye.replace_car_benefit.new_car"))

      doc.getElementsByClass("button button--next flush--left")
      doc.getElementById("remove-car-benefit-fields") should bePresent
      doc.getElementById("add-car-benefit-fields") should bePresent
    }
  }

}
