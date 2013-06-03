package controllers

import play.api.mvc.{ AnyContent, Action, Controller }
import java.util.UUID
import controllers.service.PersonalTax

object Personal extends Personal(new PersonalTax())

private[controllers] class Personal(personalTax: PersonalTax) extends Controller with ActionWrappers {

  import scala.concurrent.ExecutionContext.Implicits._

  def home = StubAuthenticatedAction {
    WithPersonalData[AnyContent] { implicit request =>
      Async {
        val employmentsFuture = personalTax.employments(request.paye.get.employments.get.toString)
        for {
          employments <- employmentsFuture
        } yield (Ok(views.html.home(employments)))
      }
    }
  }

  def test = Action {
    val uri = "/auth/oid/" + UUID.randomUUID().toString
    Ok(s"Hello, you're now logged in (with id '$uri')").withSession(("id", uri))
  }
}
