package uk.gov.hmrc.common


import scala.concurrent.ExecutionContext
import org.slf4j.MDC
import controllers.common.actions.LoggingDetails


object MdcLoggingExecutionContext{
  implicit def fromLoggingDetails(implicit loggingDetails:LoggingDetails):ExecutionContext = new MdcLoggingExecutionContext(ExecutionContext.global, loggingDetails.mdcData)
}

class MdcLoggingExecutionContext(wrapped: ExecutionContext, mdcData:Map[String, String]) extends ExecutionContext {

  def execute(runnable: Runnable) {
    wrapped.execute(new Transporter(runnable, mdcData))
  }

  private class Transporter(runnable: Runnable, mdcData:Map[String, String]) extends Runnable {
    def run(): Unit = {
      mdcData.foreach {
        case (k, v) => MDC.put(k, v)
      }
      try {
        runnable.run()
      }
      finally {
        MDC.clear()
      }
    }
  }

  def reportFailure(t: Throwable): Unit = wrapped.reportFailure(t)
}
