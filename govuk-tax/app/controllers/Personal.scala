package controllers

import play.api.mvc.{ AnyContent, Action, Controller }
import java.util.UUID
import controllers.service.PersonalTax
import scala.concurrent.Future

object Personal extends Personal(new PersonalTax())

private[controllers] class Personal(personalTax: PersonalTax) extends Controller with ActionWrappers {

  import scala.concurrent.ExecutionContext.Implicits._

  def home = StubAuthenticatedAction {
    WithPersonalData[AnyContent] { implicit request =>
      Async {
        Future(Ok(views.html.home(request.paye.get.firstName)))
      }
    }
  }

  def test = Action {
    val uri = "/auth/oid/" + UUID.randomUUID().toString
    Ok(s"Hello, you're now logged in (with id '$uri')").withSession(("id", uri))
  }
}
