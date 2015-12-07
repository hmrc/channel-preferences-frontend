package authentication

import play.api.mvc.{AnyContent, Request, Results}
import uk.gov.hmrc.play.frontend.auth._

import scala.concurrent.Future

object ValidSessionCredentialsProvider extends AnyAuthenticationProvider with Results {

  override def ggwAuthenticationProvider: GovernmentGateway = new GovernmentGateway {
    override def login: String = throw new IllegalStateException("Should be no redirect to login")
    override def redirectToLogin(redirectToOrigin: Boolean)(implicit request: Request[AnyContent]) = Future.successful(Unauthorized)
  }

  override def verifyAuthenticationProvider: Verify = new Verify {
    override def login: String = throw new IllegalStateException("Should be no redirect to login")
    override def redirectToLogin(redirectToOrigin: Boolean)(implicit request: Request[AnyContent]) = Future.successful(Unauthorized)
  }

  override def redirectToLogin(redirectToOrigin: Boolean)(implicit request: Request[AnyContent]) = Future.successful(Unauthorized)
  override def login: String = throw new IllegalStateException("Should be no redirect to login")
}
