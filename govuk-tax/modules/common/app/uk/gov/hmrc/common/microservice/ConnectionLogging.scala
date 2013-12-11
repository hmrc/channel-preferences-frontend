package uk.gov.hmrc.microservice

import scala.util.{Failure, Success, Try}
import play.api.Logger
import controllers.common.actions.HeaderCarrier
import scala.concurrent._
import ExecutionContext.Implicits.global

trait ConnectionLogging {
  self: Connector =>

  lazy val connectionLogger = Logger(s"connector.$serviceUrl")

  import ConnectionLogging.formatNs

  def withLogging[T](method: String, uri: String)(body: => Future[T])(implicit hc: HeaderCarrier): Future[T] = {
    connectionLogger.debug(s"${hc.requestId.getOrElse("")}:${formatNs(hc.elapsedNs)}")
    val startTime = System.nanoTime()
    val f = body
    f.onComplete {logResult(hc.requestId.getOrElse(""), method, uri, startTime)}
    f
  }

  def logResult[A](requestId: String, method: String, uri: String, startTime: Long)(result: Try[A]) = result match {
    case Success(ground) => {
      connectionLogger.trace(formatMessage(requestId, method, uri, System.nanoTime() - startTime, "success"))
    }
    case Failure(ex) => {
      connectionLogger.trace(formatMessage(requestId, method, uri, System.nanoTime() - startTime, s"failed ${ex.getMessage}"))
    }
  }

  def formatMessage(requestId:String, method: String, uri: String, elapsedNs: Long, message: String) =
    s"$requestId:$method:$uri:$elapsedNs:${formatNs(elapsedNs)}:$message"
}

object ConnectionLogging {
  def formatNs(ns: Long): String = {
    val nsPart = ns % 1000
    val usPart = ns / 1000 % 1000
    val msPart = ns / 1000000 % 1000
    val sPart = ns / 1000000000

    if (sPart > 0) f"${(sPart * 1000 + msPart) / 1000.0}%3.3f s"
    else if (msPart > 0) f"${(msPart * 1000 + usPart) / 1000.0}%3.3f ms"
    else if (usPart > 0) f"${(usPart * 1000 + nsPart) / 1000.0}%3.3f us"
    else s"$ns ns"
  }
}

