package controllers.paye

import views.html.paye.replace_car_benefit_review
import org.jsoup.Jsoup
import play.api.test.Helpers._
import play.api.test.{FakeApplication, WithApplication}
import scala.collection.JavaConversions._
import play.api.i18n.Messages

class ReplaceCarBenefitReviewTemplateSpec extends PayeBaseSpec {

  "replace car benefit review template" should {

    "render the old car and new car summary tables" in new WithApplication(FakeApplication()){
      implicit val user = johnDensmore
      implicit val request = requestWithCorrectVersion
      val doc = Jsoup.parse(contentAsString(replace_car_benefit_review(johnDensmoresBenefits(0), johnDensmoresEmployments(0), johnDensmoresRemovedCarBenefitFormData, johnDensmoresNewCarData)))
      doc.getElementById("company-car-details") should bePresent
      doc.getElementById("new-company-car-details") should bePresent

      val h2 = doc.getElementsByClass("benefit-type").toList.map(_.text)
      h2 should contain (Messages("paye.replace_car_benefit.benefit_type"))
      h2 should contain (Messages("paye.replace_car_benefit.new_car"))
    }
  }

}
