package controllers

import uk.gov.hmrc.play.frontend.auth.AuthContext

trait FindTaxIdentifier {
  def findTaxIdentifier(authContext: AuthContext) =
    if(authContext.principal.accounts.sa.isDefined) authContext.principal.accounts.sa.get.utr else authContext.principal.accounts.paye.get.nino
}
