import org.scalatest.concurrent.ScalaFutures
import play.api.test.TestServer
import uk.gov.hmrc.endtoend
import uk.gov.hmrc.endtoend.sa.config.TestConfig
import uk.gov.hmrc.endtoend.sa.page.PreferencesFrontEnd
import uk.gov.hmrc.endtoend.sa.page.PreferencesFrontEnd._

//TODO rename Spec & package
class UpgradeBrowserSpec extends endtoend.sa.Spec with ScalaFutures {

  val t = TestServer(9000)

  override def beforeAll() = {
    super.beforeAll()
    t.start()
  }

  override def afterAll() = {
    super.afterAll()
    t.stop()
  }

  implicit val testConfig = TestConfig(
    frontendBaseUrl = "http://localhost:9000",
    requiresPort = false,
    //TODO remove all of the following args
    saUser = null,
    nonSAUser = null,
    ninoUser = null,
    apiBaseUrl = "",
    mailgunBaseUrl = ""
  )

  feature("The generic upgrade page") {
    scenario("Agree to upgrading") {
      Given("I am on the Upgrade Page")

        go to GenericUpgradePage
        GenericUpgradePage should be (displayed)

      When("I click 'Yes' and then 'Submit")

      Then("I am taken back to the return page")

      And("My T&Cs have been set to generic=accepted")

//      `/preferences-admin/sa/individual`.delete(utr).futureValue
//
//      `/portal/preferences/sa/individual`.postPendingEmail(utr, pendingEmail).futureValue.status should be (201)
//      `/preferences-admin/sa/individual`.verifyEmailFor(utr).futureValue.status should be (204)
//
//      val activateResponse = `/preferences/sa/individual/:utr/activations`(utr).put().futureValue
//      activateResponse.status should be (412)
//
//      (activateResponse.json \ "redirectUserTo").as[JsString].value should include ("/account/account-details/sa/upgrade-email-reminders")
//
//      val upgradeResponse = `/upgrade-email-reminders`.get().futureValue
//      upgradeResponse.status should be (200)
//      upgradeResponse.body should include ("Go paperless with HMRC")
//
//      val response = `/upgrade-email-reminders`.post(optIn = true, acceptedTandC = Some(true)).futureValue
//      response should have('status(303))
//      response.header("Location").get should be (routes.UpgradeRemindersController.displayUpgradeConfirmed(Encrypted(returnUrl)).toString())
//
//      `/preferences/sa/individual/:utr/activations`(utr).put().futureValue.status should be (200)
    }
  }
}
