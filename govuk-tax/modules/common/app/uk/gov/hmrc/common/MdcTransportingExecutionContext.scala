package uk.gov.hmrc.common


import scala.concurrent.ExecutionContext
import org.slf4j.MDC
import scala.collection.JavaConverters._

class MdcTransportingExecutionContext(wrapped: ExecutionContext) extends ExecutionContext {

  def execute(runnable: Runnable) {
    MDC.getMDCAdapter.getCopyOfContextMap match {
      case null =>
        wrapped.execute(runnable)
      case context =>
        wrapped.execute(new Transporter(runnable, context.asScala.toMap.asInstanceOf[Map[String, String]]))
    }
  }

  private class Transporter(runnable: Runnable, contextScala: Map[String, String]) extends Runnable {
    def run(): Unit = {
      contextScala.foreach {
        case (k, v) => MDC.put(k, v)
      }
      try {
        runnable.run()
      }
      finally {
        contextScala.foreach {
          case (k, _) => MDC.remove(k)
        }
      }
    }
  }

  def reportFailure(t: Throwable): Unit = wrapped.reportFailure(t)
}
