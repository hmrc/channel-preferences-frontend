package uk.gov.hmrc.common.microservice

import scala.util.{Failure, Success, Try}
import play.api.Logger
import controllers.common.actions.{LoggingDetails, HeaderCarrier}
import scala.concurrent._

trait ConnectionLogging {
  self: Connector =>

  lazy val connectionLogger = Logger(s"connector.$serviceUrl")

  import ConnectionLogging.formatNs

  def withLogging[T](method: String, uri: String)(body: => Future[T])(implicit ld: LoggingDetails): Future[T] = {
    val startAge = ld.age
    val f = body
    f.onComplete {logResult(ld, method, uri, startAge)}
    f
  }

  def logResult[A](ld: LoggingDetails, method: String, uri: String, startAge: Long)(result: Try[A]) = result match {
    case Success(ground) => {
      connectionLogger.info(formatMessage(ld, method, uri, startAge, "ok"))
    }
    case Failure(ex) => {
      connectionLogger.info(formatMessage(ld, method, uri, startAge, s"failed ${ex.getMessage}"))
    }
  }

  def formatMessage(ld: LoggingDetails, method: String, uri: String, startAge: Long, message: String) = {
    val requestId = ld.requestId.getOrElse("")
    val durationNs = ld.age - startAge
    s"$requestId:$method:${startAge}:${formatNs(startAge)}:${durationNs}:${formatNs(durationNs)}:$uri:$message"
  }
}

object ConnectionLogging {
  def formatNs(ns: Long): String = {
    val nsPart = ns % 1000
    val usPart = ns / 1000 % 1000
    val msPart = ns / 1000000 % 1000
    val sPart = ns / 1000000000

    if (sPart > 0) f"${(sPart * 1000 + msPart) / 1000.0}%03.3fs"
    else if (msPart > 0) f"${(msPart * 1000 + usPart) / 1000.0}%03.3fms"
    else if (usPart > 0) f"${(usPart * 1000 + nsPart) / 1000.0}%03.3fus"
    else s"${ns}ns"
  }
}

