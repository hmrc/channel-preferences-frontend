import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatest.concurrent.ScalaFutures
import uk.gov.hmrc.endtoend
import uk.gov.hmrc.endtoend.sa.config.UserWithUtr

//TODO rename Spec & package
class UpgradeBrowserSpec extends endtoend.sa.Spec with ScalaFutures with WiremockStubSetup {
  import InternalPages._
  import Stubs._

  implicit val user = new UserWithUtr { val utr = "1111111111" }

  feature("The generic upgrade page") {
    scenario("Agree to upgrading") {
      Given("I am logged in")
        go to Auth.loginPage

      And("I have my preferences set")
        givenThat (Auth.`GET /auth/authority` willReturn (aResponse withStatus 200 withBody Auth.authorityRecordJson))
        givenThat (Preferences.`GET /preferences/sa/individual/<utr>/print-suppression` willReturn (
          aResponse withStatus 200 withBody Preferences.optedInPreferenceJson
        ))

      When("I go to the Upgrade Page")
        val upgradePage = GenericUpgradePage(returnUrl = Host.ReturnPage)
        go to upgradePage
        upgradePage should be (displayed)

      When("I click 'Yes' and then 'Submit")
        click on upgradePage.`terms and conditions checkbox`
        click on upgradePage.`continue`

      Then("I am taken back to the return page")
        Host.ReturnPage should be (displayed)

      And("My T&Cs have been set to generic=accepted")
        verify(Preferences.`POST /preferences/sa/individual/<utr>/terms-and-conditions`(genericAccepted = true))
    }
  }
}


