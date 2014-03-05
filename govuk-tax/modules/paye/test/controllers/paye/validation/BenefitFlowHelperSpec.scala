package controllers.paye.validation

import play.api.test.{FakeApplication, WithApplication, FakeRequest}

import uk.gov.hmrc.common.BaseSpec
import uk.gov.hmrc.common.microservice.domain.{RegimeRoots, User}
import uk.gov.hmrc.common.microservice.paye.domain.PayeRoot
import uk.gov.hmrc.common.microservice.auth.domain._

import controllers.paye.routes

import org.mockito.Mockito._

import BenefitFlowHelper._
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.common.microservice.paye.PayeConnector
import scala.concurrent.Future
import controllers.common.SessionKeys

class BenefitFlowHelperSpec extends BaseSpec with MockitoSugar {
  val validVersion: Int = 22
  val invalidVersion: Int = 21
  val versionUri = "/paye/AA000001/version"
  val payeRoot = PayeRoot("", "", "", None, "", "", "", Map("version" -> versionUri), Map(), Map())
  val ua = Authority("dummyAuth", Credentials(), Accounts(), None, None)
  val user = User("dummy", ua, RegimeRoots(paye = Some(payeRoot)))
  implicit val payeConnectorMock = mock[PayeConnector]

  "validateVersionNumber" should {
    "return the version to indicate the version check passed" in new WithApplication(FakeApplication()) {
      when(payeConnectorMock.version(versionUri)).thenReturn(Future.successful(validVersion))

      val session = FakeRequest().withSession(SessionKeys.npsVersion -> validVersion.toString).session
      val result = validateVersionNumber(user, session)

      result.isRight shouldBe true
      result.right.get shouldBe validVersion
    }

    "return a redirect to the home page when the version is not present in the session" in new WithApplication(FakeApplication()) {
      when(payeConnectorMock.version(versionUri)).thenReturn(Future.successful(validVersion))
      val session = FakeRequest().withSession(("foo", "bar")).session
      val result = validateVersionNumber(user, session)

      result.isLeft shouldBe true
      result.left.map{result =>
        result.header.status shouldBe 303
        result.header.headers.get("Location") shouldBe Some(routes.CarBenefitHomeController.carBenefitHome().url)
      }
    }

    "return a redirect to an error page when the version in the session does not match the current version of the user" in new WithApplication(FakeApplication()) {
      when(payeConnectorMock.version(versionUri)).thenReturn(Future.successful(validVersion))
      val session = FakeRequest().withSession(SessionKeys.npsVersion -> invalidVersion.toString).session
      val result = validateVersionNumber(user, session)

      result.isLeft shouldBe true
      result.left.map{result =>
        result.header.status shouldBe 303
        result.header.headers.get("Location") shouldBe Some(routes.VersionChangedController.versionChanged().url)
      }
    }
  }
}
