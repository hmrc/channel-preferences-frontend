package uk.gov.hmrc.common

import org.scalatest.matchers.{ ShouldMatchers, MustMatchers }
import org.scalatest.WordSpec

trait BaseSpec extends WordSpec with MustMatchers with ShouldMatchers {

  import scala.concurrent.{ Await, Future }
  import scala.concurrent.duration._

  implicit def extractAwait[A](future: Future[A]) = await[A](future, 1L)

  def await[A](future: Future[A], waitDuration: Long, timeUnit: TimeUnit = SECONDS) = Await.result(future, Duration(waitDuration, timeUnit))
}
