package controllers.sa.prefs

import controllers.common.GovernmentGateway
import uk.gov.hmrc.common.microservice.auth.domain.Accounts
import uk.gov.hmrc.common.microservice.domain.TaxRegime

object SaRegime extends TaxRegime {

  def isAuthorised(accounts: Accounts) = accounts.sa.isDefined

  val authenticationType = new GovernmentGateway {
    lazy val login: String = ExternalUrls.signIn
  }
}


