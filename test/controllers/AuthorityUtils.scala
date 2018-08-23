package controllers

import uk.gov.hmrc.domain.{Nino, SaUtr}
import uk.gov.hmrc.play.frontend.auth.connectors.domain._

object AuthorityUtils {

  def saAuthority(id: String, utr: String): Authority =
    Authority(s"/auth/oid/$id", Accounts(sa = Some(SaAccount(s"/sa/individual/$utr", SaUtr(utr)))), None, None, credentialStrength = CredentialStrength.Strong, confidenceLevel = ConfidenceLevel.L50, None, None, None, "")

  def payeAuthority(id: String, nino: String): Authority =
    Authority(s"/auth/oid/$id", Accounts(paye = Some(PayeAccount(s"/paye/individual/$nino", Nino(nino)))), None, None, credentialStrength = CredentialStrength.Strong, confidenceLevel = ConfidenceLevel.L50, None, None, None, "")

  def ninoAndPayeAuthority(id: String, utr: String, nino: String): Authority =
    Authority(s"/auth/oid/$id", Accounts(sa = Some(SaAccount(s"/sa/individual/$utr", SaUtr(utr))), paye = Some(PayeAccount(s"/paye/individual/$nino", Nino(nino)))), None, None, credentialStrength = CredentialStrength.Strong, confidenceLevel = ConfidenceLevel.L50, None, None, None, "")

  def emptyAuthority(id: String): Authority =
    Authority(s"/auth/oid/$id", Accounts(), None, None, credentialStrength = CredentialStrength.Strong, confidenceLevel = ConfidenceLevel.L50, None, None, None, "")

}
