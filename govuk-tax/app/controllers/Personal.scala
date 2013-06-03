package controllers

import play.api.mvc.{ AnyContent, Action, Controller }
import play.api.libs.ws.WS
import java.util.UUID

object Personal extends Personal

private[controllers] class Personal extends Controller with ActionWrappers {

  import scala.concurrent.ExecutionContext.Implicits._

  def home = StubAuthenticatedAction {
    WithPersonalData[AnyContent] { implicit request =>
      Async {
        val futureResponse = WS.url("http://localhost:8500" + request.paye.get).get()
        for {
          response <- futureResponse
        } yield (Ok("Hello " + request.paye.get + " " + response.status))
      }
    }
  }

  def test = Action {
    val uri = "/auth/oid/" + UUID.randomUUID().toString
    Ok(s"Hello, you're now logged in (with id '$uri')").withSession(("id", uri))
  }
}
