package controllers.domain

import uk.gov.hmrc.common.microservice.auth.domain._
import uk.gov.hmrc.domain._
import uk.gov.hmrc.common.microservice.auth.domain.VatAccount
import uk.gov.hmrc.common.microservice.auth.domain.PayeAccount
import uk.gov.hmrc.common.microservice.auth.domain.Credentials
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.common.microservice.auth.domain.CtAccount
import uk.gov.hmrc.common.microservice.auth.domain.Accounts
import uk.gov.hmrc.common.microservice.auth.domain.Authority
import uk.gov.hmrc.domain.Vrn
import uk.gov.hmrc.common.microservice.auth.domain.SaAccount

object AuthorityUtils {

  def payeAuthority(id: String, nino: String): Authority =
    Authority(s"/auth/oid/$id", Credentials(), Accounts(paye = Some(PayeAccount(s"/paye/$nino", Nino(nino)))), None, None)

  def saAuthority(id: String, utr: String): Authority =
    Authority(s"/auth/oid/$id", Credentials(), Accounts(sa = Some(SaAccount(s"/sa/individual/$utr", SaUtr(utr)))), None, None)

  def vatAuthority(id: String, vrn: String): Authority =
    Authority(s"/auth/oid/$id", Credentials(), Accounts(vat = Some(VatAccount(s"/vat/$vrn", Vrn(vrn)))), None, None)

  def ctAuthority(id: String, utr: String): Authority =
    Authority(s"/auth/oid/$id", Credentials(), Accounts(ct = Some(CtAccount(s"/ct/$utr", CtUtr(utr)))), None, None)

  def epayeAuthority(id: String, empRef: String): Authority =
    Authority(s"/auth/oid/$id", Credentials(), Accounts(epaye = Some(EpayeAccount(s"/epaye/$empRef", EmpRef(empRef)))), None, None)

  def allBizTaxAuthority(id: String, saUtr: String, ctUtr: String, vrn: String, empRef: String) =
    Authority(s"/auth/oid/$id", Credentials(), Accounts(
      sa = Some(SaAccount(s"/sa/individual/$saUtr", SaUtr(saUtr))),
      vat = Some(VatAccount(s"/vat/$vrn", Vrn(vrn))),
      ct = Some(CtAccount(s"/ct/$ctUtr", CtUtr(ctUtr))),
      epaye = Some(EpayeAccount(s"/epaye/$empRef", EmpRef(empRef)))),
     None, None)

  def emptyAuthority(id: String) = Authority(s"/auth/oid/$id", Credentials(), Accounts(), None, None)
}
