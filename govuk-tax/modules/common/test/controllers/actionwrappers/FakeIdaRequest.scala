package controllers.actionwrappers

import controllers.common.SessionTimeoutWrapper._
import java.util.UUID
import controllers.common.CookieEncryption
import uk.gov.hmrc.utils.DateTimeUtils._
import play.api.test.{WithApplication, FakeRequest}
import org.specs2.execute.AsResult
import org.specs2.execute
import play.Logger
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.common.microservice.MockMicroServicesForTests
import org.mockito.Mockito
import uk.gov.hmrc.common.microservice.auth.AuthMicroService
import play.api.mvc.{SimpleResult, Controller}


/**
 * Experimental: attempting to simplify the running of tests by allowing us to call controller
 * methods directly, with all of the action wrapper stuff set up. Still a work in progress. Don't use it yet!
 */

class PayeTest(user: User)(implicit controller: Controller with MockMicroServicesForTests) extends ActionWrapperTestHelper {

  /**
   * This class is extremely sensitive to initialisation conditions, because of the underlying
   * use of DelayedInit (grr) somewhere down below. This means we must NOT have ANY default parameters, initialisation code or
   * defined any non-lazy vals in this class. This also applies to anything that is imported here.
   *
   * defs are OK, and lazy vals are fine. Anything else breaks the world. (It really does, I once crashed the scala compiler!)
   *
   * If you get wierd compiler errors, or tests moaning that there is no running application during the test
   * then you have probably broken the initialisation code of this class.
   */
  lazy val u = user
  lazy val c = controller
  lazy val authMicroService = c.authMicroService

  def session = TestSession(
    Map(
      "sessionId" -> encrypt(s"session-${UUID.randomUUID().toString}"),
      lastRequestTimestampKey -> now.getMillis.toString
    )
  )
}

case class TestSession(values: Map[String, String])

protected trait ActionWrapperTestHelper
  extends WithApplication
  with CookieEncryption {

  protected val u: User
  protected val c: Controller with MockMicroServicesForTests
  protected val authMicroService: AuthMicroService

  // Override this in your tests if you want to modify the contents of the session for the duration of the test
  def session: TestSession

  implicit lazy val request = FakeRequest().withSession(makeSession.toSeq: _*)

  def makeSession: Map[String, String] = session.values ++ Map(
    "userId" -> encrypt(u.userId)
  )

  protected def setupActionWrapperMocks() {
    Mockito.reset(authMicroService)
    Mockito.when(authMicroService.authority(u.userId)).thenReturn(Some(u.userAuthority))
  }

  override def around[A](t: => A)(implicit e: AsResult[A]): execute.Result = try {
    setupActionWrapperMocks()
    super.around(t)
  }
  catch {
    case e: Exception =>
      Logger.error("Exception caught running test", e)
      throw e
  }
}
