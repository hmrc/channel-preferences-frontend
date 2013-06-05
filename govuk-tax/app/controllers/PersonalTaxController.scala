package controllers

import play.api.mvc.{ Action, Controller }
import java.util.UUID
import scala.concurrent.Future
import controllers.domain.PayeRegime

object PersonalTaxController extends PersonalTaxController

private[controllers] class PersonalTaxController() extends Controller with ActionWrappers {

  import scala.concurrent.ExecutionContext.Implicits._

  def newHome = AuthorisedForAction[PayeRegime] {
    implicit user =>
      implicit request =>
        Async {
          val payeData = user.regime.paye.getOrElse(throw new Exception("Regime paye not found"))

          Future(Ok(payeData.designatoryDetails.name))
        }
  }

  //  def home = StubAuthenticatedAction {
  //    WithPersonalData[AnyContent] {
  //      implicit request =>
  //        Async {
  //          Future(Ok(views.html.home(request.paye.get.firstName)))
  //        }
  //    }
  //  }

  def test = Action {
    val uri = "/auth/oid/" + UUID.randomUUID().toString
    Ok(s"Hello, you're now logged in (with id '$uri')").withSession(("id", uri))
  }
}
