package controllers.paye

import uk.gov.hmrc.common.BaseSpec
import views.PageSugar
import org.joda.time.{ LocalDate, DateTime }
import views.html.paye.paye_home
import play.api.templates.Html

class PayeTemplatesSpec extends BaseSpec with PageSugar {

  "Tax overview page" should {

    "render employments together with taxcodes" in {

      val overview: PayeOverview = PayeOverview("John Densmore", Some(new DateTime()), "CS700100A",
        List(EmploymentView("Bla Bla", new LocalDate(2009, 4, 11), Some(new LocalDate(2010, 2, 21)), "BR", List()), EmploymentView("Sansbury", new LocalDate(2010, 2, 22), None, "277T", List(RecentChange("something", new LocalDate(2013, 2, 22))))), true)
      val payeHome: Html = paye_home(overview)

      payeHome.body must include("Sansbury")

    }
  }

}
