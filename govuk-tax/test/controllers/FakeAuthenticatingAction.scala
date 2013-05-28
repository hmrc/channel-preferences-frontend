package controllers

import java.util.UUID
import play.api.mvc._
import play.api.mvc.Results._
import play.api.mvc.AsyncResult
import scala.concurrent.Future

object FakeAuthenticatingAction {

  import scala.concurrent.ExecutionContext.Implicits._

  def apply(uuid: UUID, handler: AuthenticatedRequest[AnyContent] => AsyncResult): Action[AnyContent] =
    apply(uuid, BodyParsers.parse.anyContent)(handler)

  def apply[A](uuid: UUID, bodyParser: BodyParser[A])(handler: AuthenticatedRequest[A] => AsyncResult): Action[A] = new Action[A] {
    def parser = bodyParser

    def apply(request: Request[A]): AsyncResult = Async {
      Future(handler(AuthenticatedRequest(uuid, request)))
    }
  }
}

