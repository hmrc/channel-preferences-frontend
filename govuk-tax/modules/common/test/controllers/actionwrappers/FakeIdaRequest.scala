package controllers.actionwrappers

import controllers.common.SessionTimeoutWrapper._
import java.util.UUID
import controllers.common.CookieEncryption
import uk.gov.hmrc.utils.DateTimeUtils._
import play.api.test.{WithApplication, FakeRequest}
import play.api.mvc.{Action, AnyContent, Result}
import org.specs2.execute.AsResult
import org.specs2.execute
import play.Logger

/**
 * Experimental: attempting to simplify the running of tests by allowing us to call controller
 * methods directly, with all of the action wrapper stuff set up. Still a work in progress. Don't use it yet!
 */

trait IdaLoginTestHelpers extends TestDefaults {
  lazy val testSession = TestSession(
    Map(
      "sessionId" -> encrypt(s"session-${UUID.randomUUID().toString}"),
      lastRequestTimestampKey -> now.getMillis.toString
    )
  )

  class PayeTest(action: => Action[AnyContent]) extends ActionWrapperTestHelper {

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

    import play.api.test.Helpers.contentAsString

    lazy val result: Result = action()(FakeRequest())
    lazy val content: String = contentAsString(result)

    def session = testSession
  }

}

case class TestSession(values: Map[String, String])

trait TestDefaults extends CookieEncryption {

  val testSession: TestSession

  protected trait ActionWrapperTestHelper extends WithApplication {

    def session: TestSession

    override def around[T](t: => T)(implicit e: AsResult[T]): execute.Result = try {
      super.around(t)(e)
    }
    catch {
      case e: Exception =>
        Logger.error("Exception caught running test", e)
        throw e
    }
  }

}
