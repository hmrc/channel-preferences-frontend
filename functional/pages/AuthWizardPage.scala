package pages

import org.openqa.selenium.WebDriver
import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import org.scalatest.time._
import uk.gov.hmrc.endtoend.sa.{Page, RelativeUrl}
import uk.gov.hmrc.play.config.ServicesConfig
 
case class AuthCredential(redirectUrl: String,  credentialStrength: String = "week", confidenceLevel: String = "50")

object AuthWizardPage extends AuthStubPage with ServicesConfig {
  
  override val port = 9949
  val title = "Authority Wizard"
  def relativeUrl = "auth-login-stub/gg-sign-in"
  val url = {
      val environmentProperty = Option(System.getProperty("environment")).getOrElse("local").toLowerCase
      environmentProperty match {
        case "local" ⇒ "http://localhost:9949/auth-login-stub/gg-sign-in"
        case _ ⇒ baseUrl("auth-login-stub/gg-sign-in")
      }
    }

  def submitLogin[A](credential: AuthCredential)(implicit driver: WebDriver) = {
    textField("redirectionUrl").value = credential.redirectUrl
    name("confidenceLevel").webElement.sendKeys(credential.confidenceLevel)
    name("credentialStrength").webElement.sendKeys(credential.credentialStrength)
    submit()
  }
}

trait AuthStubPage extends EnvPage with Port with RelativeUrl  { val port = 9949 }

trait EnvPage extends Page with EnvIntegrationPatience

trait Port {
  def port: Int
}

trait SignOutButton { this: Page =>
  def `sign out button` = linkText("Sign out")
}


trait EnvIntegrationPatience extends IntegrationPatience with Eventually {

  implicit abstract override val patienceConfig: PatienceConfig = {
    val (envBasedTimeout, envBasedInterval) = System.getProperty("env") match {
      case _ => (scaled(Span(15, Seconds)), scaled(Span(150, Milliseconds)))
    }
    PatienceConfig(timeout = envBasedTimeout, interval = envBasedInterval)
  }
}
