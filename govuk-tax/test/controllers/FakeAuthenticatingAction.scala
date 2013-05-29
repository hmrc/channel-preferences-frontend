package controllers

import java.util.UUID
import play.api.mvc._
import play.api.mvc.Results._
import play.api.mvc.AsyncResult
import scala.concurrent.Future
import controllers.service.TaxUserView

object FakeAuthenticatingAction {

  import scala.concurrent.ExecutionContext.Implicits._

  def apply(taxUserView: TaxUserView, handler: AuthenticatedRequest[AnyContent] => AsyncResult): Action[AnyContent] =
    apply(taxUserView, BodyParsers.parse.anyContent)(handler)

  def apply[A](taxUserView: TaxUserView, bodyParser: BodyParser[A])(handler: AuthenticatedRequest[A] => AsyncResult): Action[A] = new Action[A] {
    def parser = bodyParser

    def apply(request: Request[A]): AsyncResult = Async {
      Future(handler(AuthenticatedRequest(taxUserView, request)))
    }
  }
}

