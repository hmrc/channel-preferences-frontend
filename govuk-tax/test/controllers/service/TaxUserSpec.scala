package controllers.service

import test.BaseSpec
import controllers.service.{ TaxUserView, Auth, TaxUser }
import java.net.URI
import scala.concurrent.Future
import org.scalatest.mock.MockitoSugar

class TaxUserSpec extends BaseSpec with MockitoSugar {

  import org.mockito.Mockito._

  val mockAuth = mock[Auth]

  val taxUser = new TaxUser(mockAuth)

  "GET a tax user" should {
    "retrieve the tax user wrapped in a Future obtained from the Auth service" in {

      val pid = "/user/pid/123456789"

      val taxUserView = TaxUserView(URI.create(pid), Some(URI.create(s"/person/pid/$pid")))

      when(mockAuth.taxUser(pid)).thenReturn(Future.successful(new ResponseStub(taxUserView)))

      val actualView: TaxUserView = taxUser.get(pid)

      actualView must be(taxUserView)
    }
  }
}
