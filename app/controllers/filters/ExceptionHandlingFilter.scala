package controllers.filters

import model.Encrypted
import play.api.Logger
import play.api.mvc._
import uk.gov.hmrc.circuitbreaker.UnhealthyServiceException
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext

import scala.concurrent.{ExecutionContext, Future}

object ExceptionHandlingFilter extends Filter with Results {

  override def apply(action: (RequestHeader) => Future[Result])(rh: RequestHeader): Future[Result] = {
    implicit val ec = executionContextFromRequest(rh)

    action(rh) recoverWith {
      case unhealthyService: UnhealthyServiceException => Future.failed(unhealthyService)
      case e =>
        val urlBinder = implicitly[QueryStringBindable[Encrypted[String]]]
        urlBinder.bind("returnUrl", rh.queryString) match {
          case Some(Right(encryptedUrl)) =>
            Logger.error(message = "An error occurred when calling preferences, redirecting to returnUrl.", error = e)
            Future.successful(Results.Redirect(encryptedUrl.decryptedValue))
          case _ => Future.failed(e)
        }
    }
  }

  private def executionContextFromRequest(rh: RequestHeader): ExecutionContext = {
    val hc = HeaderCarrier.fromHeadersAndSession(rh.headers, Some(rh.session))
    MdcLoggingExecutionContext.fromLoggingDetails(hc)
  }
}
