package utils

import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatest.concurrent.Eventually
import stubs._
import uk.gov.hmrc.endtoend.sa.config.{TestEmailAddresses, UserWithUtr}

object UserSetupHelper extends Eventually{

  def setUserAsOptedIn (validEmailAddress:String)(implicit user:UserWithUtr) {
    givenThat (Auth.`GET /auth/authority` willReturn (aResponse withStatus 200 withBody Auth.authorityRecordJson))
    givenThat (EntityResolver.`GET /preferences` willReturn (
      aResponse withStatus 200 withBody EntityResolver.optedInPreferenceJson(validEmailAddress)
      ))
  }

}
