import conf.ServerSetup
import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatest.concurrent.{ScalaFutures, Eventually}
import pages.{ConfirmationOfChangedEmailAddressPage, CheckChangedEmailAddressPage, ChangeEmailAddressPage}
import uk.gov.hmrc.endtoend
import uk.gov.hmrc.endtoend.sa.config.{TestEmailAddresses, UserWithUtr}
import utils.UserSetupHelper

class ChangeEmailAddressBrowserSpec extends endtoend.sa.Spec with ServerSetup with ScalaFutures with Eventually{
  import stubs._
  implicit val user = new UserWithUtr { val utr = "1111111111" }

  feature("Change email page validation") {
    scenario("The change email page should throw a validation error if the User attempts to submit an invalid email address") {
      val validEmailAddress = TestEmailAddresses.generateSafe

      Given("I am logged in as an opted in user")
        go to Auth.loginPage
        UserSetupHelper.setUserAsOptedIn(validEmailAddress)

      And("I am on Change email address Page")
        val changeEmailAddressPage = ChangeEmailAddressPage(returnUrl = Host.ReturnPage, Host.returnLinkText)
        go to changeEmailAddressPage
        changeEmailAddressPage should be (displayed)

      When("I attempt to submit the email change form with no email")
        changeEmailAddressPage.completeForm("")

      Then("I am shown a validation error message informing me that I need to enter an email address")
        changeEmailAddressPage.`change email validation message` should include ("Enter a valid email address.")

      When("I enter an invalid email address and submit")
        changeEmailAddressPage.completeForm(TestEmailAddresses.invalidlyFormatted)

      Then("I am shown a validation error message informing me that I need to enter a valid email address")
        changeEmailAddressPage.`change email validation message` should include ("Enter a valid email address.")
    }
  }

  feature("User changes email address") {
    scenario("User is asked to confirm their new unconventional email address and can return back to correct it"){
      val validEmailAddress = TestEmailAddresses.generateSafe

      Given("I am logged in as an opted in user")
        go to Auth.loginPage
        UserSetupHelper.setUserAsOptedIn(validEmailAddress)

      And("I am on Change email address Page")
        val changeEmailAddressPage = ChangeEmailAddressPage(returnUrl = Host.ReturnPage, Host.returnLinkText)
        go to changeEmailAddressPage
        changeEmailAddressPage should be (displayed)

      When("I attempt to submit an unsafe email address")
        changeEmailAddressPage.completeForm(TestEmailAddresses.generateUnsafe)

      Then("I am asked to confirm email address is correct")
        val checkEmailAddressPage = CheckChangedEmailAddressPage(returnUrl = Host.ReturnPage, Host.returnLinkText)
        checkEmailAddressPage should be (displayed)

      When("I confirm the changed email is correct")
        click on checkEmailAddressPage.changedEmailIsNotCorrectLink

      Then("I am taken back to change email page")
        changeEmailAddressPage should be (displayed)
    }
  }
}
