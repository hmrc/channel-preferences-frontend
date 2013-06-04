package controllers

import play.api.mvc.{AnyContent, Action, Controller}
import java.util.UUID
import controllers.service.PersonalTax
import scala.concurrent.Future
import controllers.domain.PayeRegime

object PersonalTaxController extends PersonalTaxController(new PersonalTax())

private[controllers] class PersonalTaxController(personalTax: PersonalTax) extends Controller with ActionWrappers {

  import scala.concurrent.ExecutionContext.Implicits._

  def newHome = AuthorisedForAction[PayeRegime] {
    implicit userData => implicit request =>
      Async {

//        user.paye.employments
//        user.paye.benefits
//        user.ct.benefits
//
//        user.personal.paye.employments
//        user.business.paye.employeees
//        user.business.corporationTax.statement
//
//        user.personal.map {
//           p => p.paye
//        }
//
//        userData.userAuthority

        Future(Ok())
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
