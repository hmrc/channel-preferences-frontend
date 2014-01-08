package controllers.paye

import play.api.test.WithApplication
import views.html.paye.{add_car_benefit_confirmation_table, replace_car_benefit_form}
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
import uk.gov.hmrc.common.microservice.paye.domain.{AddCarBenefitConfirmationData, CarBenefit}
import org.scalatest.LoneElement
import scala.collection.JavaConversions
import JavaConversions._
import play.api.i18n.Messages
import org.joda.time.LocalDate
import org.joda.time.chrono.ISOChronology


class AddCarBenefitConfirmationTableTemplateSpec extends PayeBaseSpec  with MockedTaxYearSupport with LoneElement {

  val bePresent = not be null

  "add car benefit confirmation table template" should {

    implicit val user = johnDensmore

    "render a table summarising all the values added" in new WithApplication(FakeApplication()) {
      val data = AddCarBenefitConfirmationData("employerName", LocalDate(2014,1,8), 1234, "diesel", Some(222), Some("2000"),
                                               Some("date"), Some(LocalDate(2014, 2, 5)), Some(50), Some(LocalDate(2000,1,1)))
      val doc = Jsoup.parse(contentAsString(add_car_benefit_confirmation_table(data)))

      doc.getElementById("new-company-car-details") should bePresent

      println(doc.getElementsByTag("td"))

    }
  }

  object LocalDate {
    def apply(year: Int, month: Int, day: Int) = new LocalDate(year, month, day, ISOChronology.getInstanceUTC)
  }

}
