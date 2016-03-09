import com.github.tomakehurst.wiremock.client.WireMock._
import conf.ServerSetup
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import pages.GoPaperlessPage
import uk.gov.hmrc.endtoend
import uk.gov.hmrc.endtoend.sa.config.{TestEmailAddresses, UserWithUtr}

class GoPaperlessBrowserSpec extends endtoend.sa.Spec with ScalaFutures with ServerSetup with Eventually{
  import stubs._

  implicit val user = new UserWithUtr { val utr = "1111111111" }

  feature("Opting in a user") {
      scenario("I enter an invalid email address, am asked to re-enter it and then I opt-in") {
        Given("I am logged in")
          eventually {
            go to Auth.loginPage
          }
        When("I am on opt in to paperless Page")
          givenThat (Auth.`GET /auth/authority` willReturn (aResponse withStatus 200 withBody Auth.authorityRecordJson))
          val goPaperlessPage = GoPaperlessPage(returnUrl = Host.ReturnPage, Host.returnLinkText)
          go to goPaperlessPage
          goPaperlessPage should be (displayed)

        And("I enter an invalid email address")
          goPaperlessPage.completeForm(TestEmailAddresses.invalidlyFormatted)

        Then("I see an error informing me of invalid email addresses")
          goPaperlessPage.`go paperless validation message` should include ("Enter a valid email address.")

        And("I can update the invalid email address to valid one and submit the email address")
          val validEmail = TestEmailAddresses.generateSafe
          givenThat(Email.`GET /validate-email-address`(validEmail) willReturn(aResponse withStatus 200 withBody Email.validEmailJson))
          givenThat(EntityResolver.`POST /preferences/sa/individual/<utr>/terms-and-conditions` willReturn (aResponse withStatus 200))
          goPaperlessPage.completeForm(validEmail)
          pageSource should include ("Nearly done...")
      }

      scenario("I can toggle between yes and no and email parts of the form are hidden when no is selected"){
        Given("I am logged in")
          go to Auth.loginPage

        And("I am on opt in to paperless Page")
          givenThat (Auth.`GET /auth/authority` willReturn (aResponse withStatus 200 withBody Auth.authorityRecordJson))
          val goPaperlessPage = GoPaperlessPage(returnUrl = Host.ReturnPage, Host.returnLinkText)
          go to goPaperlessPage
          goPaperlessPage should be (displayed)

        When("I choose the no option")
          click on goPaperlessPage.`no I don't want to sign up radio button`

        Then("the email form should be hidden")
          goPaperlessPage.emailReminderFormClass should include ("js-hidden")

        When("I choose the yes option")
          click on goPaperlessPage.`yes send by email radio button`

        Then("the email form should be shown")
          goPaperlessPage.emailReminderFormClass should not include "js-hidden"
      }
    }

    feature("Go paperless page validation") {
      scenario("email address validation"){
        Given("I am logged in")
          go to Auth.loginPage

        When("I am on opt in to paperless Page")
          givenThat (Auth.`GET /auth/authority` willReturn (aResponse withStatus 200 withBody Auth.authorityRecordJson))
          val goPaperlessPage = GoPaperlessPage(returnUrl = Host.ReturnPage, Host.returnLinkText)
          go to goPaperlessPage
          goPaperlessPage should be (displayed)

        And("I enter a blank email address")
          goPaperlessPage.completeForm("")

        Then("I see an error informing me of invalid email addresses")
          goPaperlessPage.`go paperless validation message` should include ("Enter a valid email address.")

        When("I attempt to opt in with email addresses that don't match")
          goPaperlessPage.completeForm(TestEmailAddresses.generateSafe, confirmEmail= Some(TestEmailAddresses.generateSafe))

        Then("I receive a validation error and I stay on the page")
          goPaperlessPage.`go paperless validation message` should include ("Check your email addresses - they don’t match.")

        When("I attempt to log in with matching but invalid email addresses")
          goPaperlessPage.completeForm(TestEmailAddresses.invalidlyFormatted, confirmEmail= Some(TestEmailAddresses.invalidlyFormatted))

        Then("I receive a validation error and I stay on the page")
          goPaperlessPage.`go paperless validation message` should include ("Enter a valid email address.")
      }


      scenario("Terms and Conditions validation") {
        Given("I am logged in")
          go to Auth.loginPage

        And("I am on opt in to paperless Page")
          givenThat (Auth.`GET /auth/authority` willReturn (aResponse withStatus 200 withBody Auth.authorityRecordJson))
          val goPaperlessPage = GoPaperlessPage(returnUrl = Host.ReturnPage, Host.returnLinkText)
          go to goPaperlessPage
          goPaperlessPage should be(displayed)

        When("I attempt to opt in with no email set and the terms not selected")
          goPaperlessPage.completeForm("", TsAndCsSelected = false)

        Then("I receive a validation error informing me I haven't agreed to the terms")
          goPaperlessPage.`go paperless validation message` should include("You must accept the terms and conditions")

        When("I enter a valid email address but don't select T's and C's checkbox")
          goPaperlessPage.completeForm(TestEmailAddresses.generateSafe, TsAndCsSelected = false)

        Then("I receive a validation error informing me I haven't agreed to the terms")
          goPaperlessPage.`go paperless validation message` should include("You must accept the terms and conditions")

        When("I enter email addresses that don't match and I don't select T's and C's checkbox")
          goPaperlessPage.completeForm(TestEmailAddresses.generateSafe, confirmEmail = Some(TestEmailAddresses.generateSafe), TsAndCsSelected = false)

        Then("I receive a validation error informing me I haven't agreed to the terms")
          goPaperlessPage.`go paperless validation message` should include("You must accept the terms and conditions")

      }
    }

}
