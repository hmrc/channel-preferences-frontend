package uk.gov.hmrc.common

import org.scalatest._

trait BaseSpec extends WordSpec with Matchers with BeforeAndAfterEachTestData with BeforeAndAfter {

  import scala.concurrent.{ Await, Future }
  import scala.concurrent.duration._

  implicit def extractAwait[A](future: Future[A]) = await[A](future, 1L)

  def await[A](future: Future[A], waitDuration: Long, timeUnit: TimeUnit = SECONDS) = Await.result(future, Duration(waitDuration, timeUnit))
}
