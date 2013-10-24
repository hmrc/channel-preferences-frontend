package uk.gov.hmrc.common.microservice.governmentgateway

import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.common.BaseSpec
import org.mockito.Mockito._
import play.api.test.WithApplication
import play.api.test.FakeApplication
import scala.Some

class TestGovernmentGatewayMicroService extends GovernmentGatewayMicroService with MockitoSugar {

  val httpWrapper = mock[HttpWrapper]

  override protected def httpGet[A](uri: String)(implicit m: Manifest[A]): Option[A] = {
    httpWrapper.get[A](uri)
  }

  class HttpWrapper {
    def get[T](uri: String): Option[T] = None
  }

}

class GovernmentGatewayMicroServiceSpec extends BaseSpec with MockitoSugar {

  lazy val service = new TestGovernmentGatewayMicroService

  "profile endpoint" should {

    "return the user profile when details are found in government gateway microservice" in new WithApplication(FakeApplication()) {

      val userId = "/auth/oid/geofffisher"
      val expectedResponse = Some(JsonProfileResponse(affinityGroup = "Organisation", activeEnrolments = Set("HMCE-EBTI-ORG","HMRC-EMCS-ORG")))
      val expectedResult = Some(ProfileResponse(
        affinityGroup = AffinityGroup("organisation"),
        activeEnrolments = Set(
          Enrolment("HMCE-EBTI-ORG"),
          Enrolment("HMRC-EMCS-ORG"))))

      when(service.httpWrapper.get[JsonProfileResponse](s"/profile$userId")).thenReturn(expectedResponse)
      val result = service.profile(userId)
      result shouldBe expectedResult
    }

    "return None when profile for given user is not found" in new WithApplication(FakeApplication()) {

      val userId = "/auth/oid/missingUser"
      val expectedResponse = None

      when(service.httpWrapper.get[JsonProfileResponse](s"/profile$userId")).thenReturn(expectedResponse)
      val result = service.profile(userId)
      result shouldBe expectedResponse
    }

  }

}
