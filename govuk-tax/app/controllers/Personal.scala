package controllers

import play.api.mvc.Controller
import play.api.libs.ws.WS

object Personal extends Personal

private[controllers] class Personal extends Controller with ActionWrappers {

  import scala.concurrent.ExecutionContext.Implicits._

  def home = AuthenticatedPersonAction { implicit request =>
    Async {
      val futureResponse = WS.url("http://www.foo.com/user/pid/" + request.personId).get()
      for {
        response <- futureResponse
      } yield (Ok("Hello"))
    }
  }
}
