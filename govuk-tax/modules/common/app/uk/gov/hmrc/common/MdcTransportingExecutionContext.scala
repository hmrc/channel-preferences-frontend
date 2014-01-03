package uk.gov.hmrc.common


import scala.concurrent.ExecutionContext
import org.slf4j.MDC
import scala.collection.JavaConverters._

class MdcTransportingExecutionContext(wrapped: ExecutionContext) extends ExecutionContext {
  def execute(runnable: Runnable): Unit = {
    MDC.getMDCAdapter.getCopyOfContextMap match {
      case null => wrapped.execute(runnable)
      case context =>
        val contextScala = context.asScala.toMap.asInstanceOf[Map[String, String]]
        wrapped.execute(new Runnable {
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
        })
    }
  }

  def reportFailure(t: Throwable): Unit = wrapped.reportFailure(t)
}
