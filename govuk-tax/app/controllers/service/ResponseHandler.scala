package controllers.service

import play.api.http.Status
import play.api.libs.ws.Response

trait ResponseHandler extends Status {

  import controllers.domain.Transform._
  import scala.concurrent.{ ExecutionContext, Future }
  import ExecutionContext.Implicits.global

  case class Statuses(r: Range) {
    def unapply(i: Int): Boolean = r contains i
  }

  val success = Statuses(OK to MULTI_STATUS)

  def response[A](futureResponse: Future[Response])(implicit m: Manifest[A]): Future[A] = {
    futureResponse map {
      res =>
        res.status match {
          case OK => fromResponse[A](res.body)
          //          case success() => //do nothing

          //TODO: add some proper error handling
          case BAD_REQUEST => throw new RuntimeException("Bad request")
          case UNAUTHORIZED => throw new RuntimeException("Unauthenticated request")
          case FORBIDDEN => throw new RuntimeException("Not authorised to make this request")
          case NOT_FOUND => throw new RuntimeException("Resource not found")
          case CONFLICT => throw new RuntimeException("Invalid state")
          case _ => throw new RuntimeException("Internal server error")
        }
    }
  }
}

