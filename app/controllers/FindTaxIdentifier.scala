package controllers

import uk.gov.hmrc.domain.{Nino, SaUtr}
import uk.gov.hmrc.play.frontend.auth.AuthContext

trait FindTaxIdentifier {
  def findTaxIdentifier(authContext: AuthContext) =
    (authContext.principal.accounts.sa.isDefined, authContext.principal.accounts.paye.isDefined) match {
      case (true, _) => authContext.principal.accounts.sa.get.utr
      case (_, true) => authContext.principal.accounts.paye.get.nino
      case _ => throw new RuntimeException("No supported tax regimes present i.e, SA or Paye")
    }

  def findUtr(authContext: AuthContext) : Option[SaUtr] = authContext.principal.accounts.sa.map(_.utr)

  def findNino(authContext: AuthContext) : Option[Nino] = authContext.principal.accounts.paye.map(_.nino)
}
