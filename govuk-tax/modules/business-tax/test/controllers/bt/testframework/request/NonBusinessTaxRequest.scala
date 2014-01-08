package controllers.bt.testframework.request

import org.mockito.Mockito._
import controllers.bt.testframework.fixtures.NonBusinessUserFixture
import controllers.common.CookieCrypto
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

trait NonBusinessTaxRequest extends CookieCrypto with NonBusinessUserFixture with MockitoSugar {

  private val userAuthority = Authority(s"/auth/oid/$userId", Credentials(), Accounts(), lastLoginTimestamp, lastLoginTimestamp, CreationAndLastModifiedDetail())

  implicit val user = User(
    userId = userAuthority.uri,
    userAuthority = userAuthority,
    regimes = RegimeRoots())

  val mockAuthConnector = mock[AuthConnector]

  when(mockAuthConnector.authority(userId)(HeaderCarrier())).thenReturn(Future.successful(Some(userAuthority)))

  def request = {

    val session: Seq[(String, Option[String])] = Seq(
      "sessionId" -> Some(encrypt(s"session-${UUID.randomUUID().toString}")),
      lastRequestTimestampKey -> lastRequestTimestamp.map(_.getMillis.toString),
      "userId" -> Some(encrypt(userId)))

    val cleanSession = session.collect {
      case (paramName, Some(paramValue)) => (paramName, paramValue)
    }

    FakeRequest().withSession(cleanSession: _*)
  }
}
