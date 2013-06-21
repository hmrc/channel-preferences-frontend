package controllers

import test.BaseSpec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.mock.MockitoSugar
import play.api.test.{ FakeRequest, WithApplication }
import microservices.MockMicroServicesForTests
import microservice.auth.AuthMicroService
import org.mockito.Mockito._
import microservice.sa.domain._
import microservice.auth.domain.UserAuthority
import play.api.test.FakeApplication
import scala.Some
import play.api.mvc.{AnyContent, Action, Cookie}
import microservice.sa.SaMicroService

class SaControllerErrorPageSpec extends BaseSpec with ShouldMatchers with MockitoSugar {

  import play.api.test.Helpers._

  private val mockAuthMicroService = mock[AuthMicroService]

  when(mockAuthMicroService.authority("/auth/oid/gfisher")).thenReturn(
    Some(UserAuthority("someIdWeDontCareAboutHere", Map("paye" -> "/personal/paye/DF334476B", "sa" -> "/personal/sa/123456789012"))))

  private val mockSaMicroService = mock[SaMicroService]

  when(mockSaMicroService.root("/personal/sa/123456789012")).thenReturn(
    SaRoot(
      utr = "123456789012",
      links = Map(
        "personalDetails" -> "/personal/sa/123456789012/personalDetails")
    )
  )



  private def controller = new SaController with MockMicroServicesForTests {
    override val authMicroService = mockAuthMicroService
    override val saMicroService = mockSaMicroService
  }
  //todo test what happens if user is not authorised to be in this regime - at the time of writing front-end does not do a check

  "The home method" should {

    "display an error page if personal details do not come back from backend service" in new WithApplication(FakeApplication()) {

      when(mockSaMicroService.person("/personal/sa/123456789012/personalDetails")).thenReturn(None)
      val result = controller.home(FakeRequest().withCookies(Cookie("userId", "/auth/oid/gfisher")))

      status(result) should be(404)
      

    }
  }

  "The details page" should {
    "show the individual SA address of Geoff Fisher" in {
      when(mockSaMicroService.person("/personal/sa/123456789012/personalDetails")).thenReturn(None)
      val result = controller.details(FakeRequest().withCookies(Cookie("userId", "/auth/oid/gfisher")))
      status(result) should be(404)
    }
  }


}
