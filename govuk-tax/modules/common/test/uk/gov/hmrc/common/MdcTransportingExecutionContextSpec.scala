package uk.gov.hmrc.common

import scala.concurrent.{Await, ExecutionContext, future}
import org.slf4j.MDC
import scala.reflect._
import ch.qos.logback.classic.spi.ILoggingEvent
import org.slf4j.LoggerFactory
import ch.qos.logback.classic.{Level, Logger => LogbackLogger}
import scala.collection.JavaConverters._
import org.scalatest.{Inspectors, LoneElement}
import scala.concurrent.duration._
import java.util.concurrent.Executors
import scala.collection.mutable
import ch.qos.logback.core.AppenderBase

class MdcTransportingExecutionContextSpec extends BaseSpec with LoneElement with Inspectors {

  "The MDC Transporting Execution Context" should {
    "capture the MDC map and put it in place when a task is run" in withCaptureOfLoggingFrom[LogsSomethingInAFuture] {
      logList =>
        implicit val ec = new MdcTransportingExecutionContext(ExecutionContext.fromExecutor(Executors.newFixedThreadPool(1)))
        Await.ready(future {
          /* does nothing but makes sure the new thread is created */
        }, 2 second)

        MDC.put("someKey", "something")

        Await.ready(new LogsSomethingInAFuture().doIt(), 2 second)

        logList.loneElement._2 should contain("someKey" -> "something")
    }
  }

  def withCaptureOfLoggingFrom[T: ClassTag](body: (=> List[(ILoggingEvent, Map[String, String])]) => Any) = {
    val logger = LoggerFactory.getLogger(classTag[T].runtimeClass).asInstanceOf[LogbackLogger]
    val appender = new AppenderBase[ILoggingEvent]() {

      val list = mutable.ListBuffer[(ILoggingEvent, Map[String, String])]()

      override def append(e: ILoggingEvent): Unit = {
        println("Capturing a log message: " + Thread.currentThread().getName)
        list.append((e, e.getMDCPropertyMap().asScala.toMap))
      }
    }
    appender.setContext(logger.getLoggerContext)
    appender.start()
    logger.addAppender(appender)
    logger.setLevel(Level.ALL)
    logger.setAdditive(true)
    body(appender.list.toList)
  }
}

class LogsSomethingInAFuture {

  def doIt()(implicit ec: ExecutionContext) = future {
    LoggerFactory.getLogger(this.getClass).info("here is a message")
  }
}

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
            runnable.run()
            //        context.foreach { case(k, _) => MDC.remove(k) }
          }
        })
    }
  }

  def reportFailure(t: Throwable): Unit = wrapped.reportFailure(t)
}
