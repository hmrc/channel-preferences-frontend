package controllers.sa.prefs

import uk.gov.hmrc.common.microservice.auth.domain.{Accounts, Credentials, SaAccount, Authority}
import uk.gov.hmrc.domain.SaUtr

object AuthorityUtils {

  def saAuthority(id: String, utr: String): Authority =
    Authority(s"/auth/oid/$id", Credentials(), Accounts(sa = Some(SaAccount(s"/sa/individual/$utr", SaUtr(utr)))), None, None)

}
