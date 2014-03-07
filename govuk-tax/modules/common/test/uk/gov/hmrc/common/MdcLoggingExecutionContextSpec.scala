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
import java.util.concurrent.{CyclicBarrier, CountDownLatch, Executors}
import scala.collection.mutable
import ch.qos.logback.core.AppenderBase
import controllers.common.actions.{LoggingDetails, HeaderCarrier}
import play.core.NamedThreadFactory
import controllers.common.HeaderNames

class MdcLoggingExecutionContextSpec extends BaseSpec with LoneElement with Inspectors with BeforeAndAfter {

  before {
    MDC.clear()
  }

  "The MDC Transporting Execution Context" should {

    "capture an MDC map using implicit from LoggingDetails" in withCaptureOfLoggingFrom[MdcLoggingExecutionContextSpec]{
      logList =>

        import MdcLoggingExecutionContext._

        implicit val loggingDetails:LoggingDetails = HeaderCarrier().copy(requestId = Some("rid"))

        //WIP Charles - find how to make this call without parameters.
        logEventInsideAFutureUsingImplicitEc(fromLoggingDetails(loggingDetails))

        logList.map(_._2).loneElement should contain(HeaderNames.xRequestId -> "rid")
      }

    "capture the an MDC map with values in it and put it in place when a task is run" in withCaptureOfLoggingFrom[MdcLoggingExecutionContextSpec] {
      logList =>
        implicit val ec = createAndInitialiseMdcTransportingExecutionContext(Map("someKey" -> "something"))

        logEventInsideAFutureUsing(ec)

        logList.loneElement._2 should contain("someKey" -> "something")
    }

    "ignore an null MDC map" in withCaptureOfLoggingFrom[MdcLoggingExecutionContextSpec] {
      logList =>
        implicit val ec = createAndInitialiseMdcTransportingExecutionContext(Map())

        logEventInsideAFutureUsing(ec)

        logList.loneElement._2 should be(empty)
    }

    "clear the MDC map after a task is run" in withCaptureOfLoggingFrom[MdcLoggingExecutionContextSpec] {
      logList =>
        implicit val ec = createAndInitialiseMdcTransportingExecutionContext(Map("someKey" -> "something"))

        doSomethingInsideAFutureButDontLog(ec)

        MDC.clear()
        logEventInsideAFutureUsing(ec)

        logList.loneElement._2 should be (Map("someKey" -> "something"))
    }

    "clear the MDC map after a task throws an exception" in withCaptureOfLoggingFrom[MdcLoggingExecutionContextSpec] {
      logList =>
        implicit val ec = createAndInitialiseMdcTransportingExecutionContext(Map("someKey" -> "something"))

        throwAnExceptionInATaskOn(ec)

        MDC.clear()
        logEventInsideAFutureUsing(ec)

        logList.loneElement._2 should be (Map("someKey" -> "something"))
    }
    
    "log values from given MDC map when multiple threads are using it concurrently by ensuring each log from each thread has been logged via MDC" in withCaptureOfLoggingFrom[MdcLoggingExecutionContextSpec]{
      logList =>
        val threadCount = 10
        val logCount = 10
      
        val concurrentThreadsEc = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(threadCount, new NamedThreadFactory("LoggerThread")))
        val startLatch = new CountDownLatch(threadCount)
        val completionLatch = new CountDownLatch(threadCount)
      
        for(t <- 0 until threadCount){
          future{
            MDC.clear()
            startLatch.countDown()
            startLatch.await()
            
            for(l <- 0 until logCount){
              val mdc = Map("entry" -> s"${Thread.currentThread().getName}-$l")
              logEventInsideAFutureUsing(new MdcLoggingExecutionContext(ExecutionContext.global, mdc))
            }

            completionLatch.countDown()
          }(concurrentThreadsEc)
        }

        completionLatch.await()

        val logs = logList.map(_._2).map(_.head._2).toSet
        logs.size should be(threadCount * logCount)

        for(t <- 1 until threadCount){
          for(l <- 0 until logCount){
            logs should contain(s"LoggerThread-$t-$l")
          }
        }
    }
  }


  def createAndInitialiseMdcTransportingExecutionContext(mdcData:Map[String, String]): MdcLoggingExecutionContext = {
    val ec = new MdcLoggingExecutionContext(ExecutionContext.fromExecutor(Executors.newFixedThreadPool(1)), mdcData)
    initialise(ec)
    ec
  }

  def logEventInsideAFutureUsingImplicitEc(implicit ec: ExecutionContext) {
    logEventInsideAFutureUsing(ec)
  }

  def logEventInsideAFutureUsing(ec: ExecutionContext) {
    Await.ready(future {
      LoggerFactory.getLogger(classOf[MdcLoggingExecutionContextSpec]).info("")
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


