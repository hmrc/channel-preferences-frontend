package uk.gov.hmrc.common

import scala.concurrent.Future
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.microservice.Connector
import controllers.common.actions.HeaderCarrier

class HttpWrapper {
  def get[T](uri: String): Option[T] = None

  def getF[T](uri: String): Future[Option[T]] = Future.successful(None)
}

trait MockGet extends MockitoSugar {
  self: Connector =>

  val mockHttpClient: HttpWrapper = mock[HttpWrapper]

  override def httpGet[A](uri: String)(implicit m: Manifest[A]): Option[A] = mockHttpClient.get[A](uri)

  override def httpGetF[A](uri: String)(implicit m: Manifest[A], headerCarrier: HeaderCarrier): Future[Option[A]] = mockHttpClient.getF[A](uri)
}