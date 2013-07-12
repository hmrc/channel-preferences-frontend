package controllers

import test.BaseSpec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.mock.MockitoSugar
import play.api.test.{ FakeRequest, WithApplication }
import microservices.MockMicroServicesForTests
import microservice.auth.AuthMicroService
import org.mockito.Mockito._
import microservice.sa.domain._
import microservice.auth.domain.{ Regimes, UserAuthority }
import play.api.test.FakeApplication
import scala.Some
import play.api.mvc.{ AnyContent, Action, Cookie }
import microservice.sa.SaMicroService
import org.joda.time.DateTime

class SaControllerErrorPageSpec extends BaseSpec with ShouldMatchers with MockitoSugar with CookieEncryption {

  import play.api.test.Helpers._

  private val mockAuthMicroService = mock[AuthMicroService]

  when(mockAuthMicroService.authority("/auth/oid/gfisher")).thenReturn(
    Some(UserAuthority("someIdWeDontCareAboutHere", Regimes(Some("/personal/paye/DF334476B"), Some("/personal/sa/123456789012")), Some(new DateTime(2000L)))))

  private val mockSaMicroService = mock[SaMicroService]

  when(mockSaMicroService.root("/personal/sa/123456789012")).thenReturn(
    SaRoot(
      utr = "123456789012",
      links = Map(
        "personalDetails" -> "/personal/sa/123456789012/details")
    )
  )

  private def controller = new SaController with MockMicroServicesForTests {
    override val authMicroService = mockAuthMicroService
    override val saMicroService = mockSaMicroService
  }

  "The home method" should {

    "display an error page if personal details do not come back from backend service" in new WithApplication(FakeApplication()) {

      when(mockSaMicroService.person("/personal/sa/123456789012/details")).thenReturn(None)
      val result = controller.home(FakeRequest().withSession(("userId", encrypt("/auth/oid/gfisher"))))

      status(result) should be(404)

    }
  }

  "The details page" should {
    "display an error page if personal details do not come back from backend service" in new WithApplication(FakeApplication()) {
      when(mockSaMicroService.person("/personal/sa/123456789012/details")).thenReturn(None)
      val result = controller.details(FakeRequest().withSession(("userId", encrypt("/auth/oid/gfisher"))))
      status(result) should be(404)
    }
  }

}
