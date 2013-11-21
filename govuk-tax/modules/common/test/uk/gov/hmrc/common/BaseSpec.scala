package uk.gov.hmrc.common

import org.scalatest._
import org.scalatest.mock.MockitoSugar
import controllers.common.actions.HeaderCarrier

trait BaseSpec extends org.scalatest.WordSpecLike with Matchers with BeforeAndAfterEachTestData with BeforeAndAfter with MockitoSugar {

  import scala.concurrent.{Await, Future}
  import scala.concurrent.duration._

  lazy implicit val hc = HeaderCarrier()
  implicit def extractAwait[A](future: Future[A]) = await[A](future, 1L)

  def await[A](future: Future[A], waitDuration: Long = 5, timeUnit: TimeUnit = SECONDS) = Await.result(future, Duration(waitDuration, timeUnit))

  // Convenience to avoid having to wrap andThen() parameters in Future.successful
  implicit def liftFuture[A](v: A) = Future.successful(v)

  def anyOfType[T](implicit manifest: Manifest[T]): T = org.mockito.Matchers.any(manifest.runtimeClass).asInstanceOf[T]
}

