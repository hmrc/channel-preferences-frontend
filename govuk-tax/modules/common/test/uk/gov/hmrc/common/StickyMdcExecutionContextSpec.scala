package uk.gov.hmrc.common

import scala.concurrent.{Await, ExecutionContext, future}
import org.slf4j.MDC
import scala.reflect._
import ch.qos.logback.classic.spi.ILoggingEvent
import org.slf4j.LoggerFactory
import ch.qos.logback.classic.{Level, Logger => LogbackLogger}
import scala.collection.JavaConverters._
import org.scalatest.{BeforeAndAfter, Inspectors, LoneElement}
import scala.concurrent.duration._
import java.util.concurrent.Executors
import scala.collection.mutable
import ch.qos.logback.core.AppenderBase

class StickyMdcExecutionContextSpec extends BaseSpec with LoneElement with Inspectors with BeforeAndAfter {

  before {
    MDC.clear()
  }

  "The MDC Transporting Execution Context" should {

    "capture the an MDC map with values in it and put it in place when a task is run" in withCaptureOfLoggingFrom[StickyMdcExecutionContextSpec] {
      logList =>
        implicit val ec = createAndInitialiseMdcTransportingExecutionContext()

        MDC.put("someKey", "something")
        logEventInsideAFutureUsing(ec)

        logList.loneElement._2 should contain("someKey" -> "something")
    }

    "ignore an null MDC map" in withCaptureOfLoggingFrom[StickyMdcExecutionContextSpec] {
      logList =>
        implicit val ec = createAndInitialiseMdcTransportingExecutionContext()

        logEventInsideAFutureUsing(ec)

        logList.loneElement._2 should be(empty)
    }

    "clear the MDC map after a task is run" in withCaptureOfLoggingFrom[StickyMdcExecutionContextSpec] {
      logList =>
        implicit val ec = createAndInitialiseMdcTransportingExecutionContext()

        MDC.put("someKey", "something")
        doSomethingInsideAFutureButDontLog(ec)

        MDC.clear()
        logEventInsideAFutureUsing(ec)

        logList.loneElement._2 should be (empty)
    }

    "clear the MDC map after a task throws an exception" in withCaptureOfLoggingFrom[StickyMdcExecutionContextSpec] {
      logList =>
        implicit val ec = createAndInitialiseMdcTransportingExecutionContext()

        MDC.put("someKey", "something")
        throwAnExceptionInATaskOn(ec)

        MDC.clear()
        logEventInsideAFutureUsing(ec)

        logList.loneElement._2 should be (empty)
    }
  }


  def createAndInitialiseMdcTransportingExecutionContext(): StickyMdcExecutionContext = {
    val ec = new StickyMdcExecutionContext(ExecutionContext.fromExecutor(Executors.newFixedThreadPool(1)))
    initialise(ec)
    ec
  }

  def logEventInsideAFutureUsing(ec: ExecutionContext) {
    Await.ready(future {
      LoggerFactory.getLogger(classOf[StickyMdcExecutionContextSpec]).info("")
    }(ec), 2 second)
  }

  def doSomethingInsideAFutureButDontLog(ec: ExecutionContext) {
    Await.ready(future { }(ec), 2 second)
  }

  def throwAnExceptionInATaskOn(ec: ExecutionContext) {
    ec.execute(new Runnable() {
      def run(): Unit = throw new RuntimeException("Test what happens when a task running on this EC throws an exception")
    })
  }

  /** Ensures that a thread is already created in the execution context by running an empty future.
    * Required as otherwise the MDC is transferred to the new thread as it is stored in an inheritable
    * ThreadLocal.
    */
  def initialise(ec: ExecutionContext) {
    Await.ready(future {}(ec), 2 second)
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


