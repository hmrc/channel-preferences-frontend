package controllers.bt.testframework.request

import org.mockito.Mockito._
import controllers.bt.testframework.fixtures.NonBusinessUserFixture
import java.util.UUID
import controllers.common.SessionTimeoutWrapper._
import uk.gov.hmrc.common.microservice.auth.domain._
import play.api.test.FakeRequest
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import org.scalatest.mock.MockitoSugar
import controllers.common.actions.HeaderCarrier
import scala.concurrent.Future
import uk.gov.hmrc.common.microservice.auth.domain.Credentials
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.common.microservice.domain.RegimeRoots
import uk.gov.hmrc.common.microservice.auth.domain.Accounts
import uk.gov.hmrc.common.microservice.auth.domain.Authority
import scala.Some
import controllers.common.SessionKeys

trait NonBusinessTaxRequest extends NonBusinessUserFixture with MockitoSugar {

  private val userAuthority = Authority(s"/auth/oid/$userId", Credentials(), Accounts(), lastLoginTimestamp, lastLoginTimestamp)

  implicit val user = User(
    userId = userAuthority.uri,
    userAuthority = userAuthority,
    regimes = RegimeRoots())

  val mockAuthConnector = mock[AuthConnector]

  when(mockAuthConnector.currentAuthority(HeaderCarrier())).thenReturn(Future.successful(Some(userAuthority)))

  def request = {

    val session: Seq[(String, Option[String])] = Seq(
      SessionKeys.sessionId -> Some(s"session-${UUID.randomUUID().toString}"),
      SessionKeys.lastRequestTimestamp -> lastRequestTimestamp.map(_.getMillis.toString),
      SessionKeys.userId -> Some(userId))

    val cleanSession = session.collect {
      case (paramName, Some(paramValue)) => (paramName, paramValue)
    }

    FakeRequest().withSession(cleanSession: _*)
  }
}
