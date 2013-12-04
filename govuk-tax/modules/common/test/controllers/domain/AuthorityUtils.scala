package controllers.domain

import uk.gov.hmrc.common.microservice.auth.domain._
import uk.gov.hmrc.domain._
import uk.gov.hmrc.common.microservice.auth.domain.VatAccount
import uk.gov.hmrc.common.microservice.auth.domain.PayeAccount
import uk.gov.hmrc.domain.Uar
import uk.gov.hmrc.common.microservice.auth.domain.Credentials
import uk.gov.hmrc.common.microservice.auth.domain.AgentAccount
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.common.microservice.auth.domain.CtAccount
import uk.gov.hmrc.common.microservice.auth.domain.Accounts
import uk.gov.hmrc.common.microservice.auth.domain.Authority
import scala.Some
import uk.gov.hmrc.domain.Vrn
import uk.gov.hmrc.common.microservice.auth.domain.SaAccount

object AuthorityUtils {

  def payeAuthority(id: String, nino: String): Authority =
    Authority(s"/auth/oid/$id", Credentials(), Accounts(paye = Some(PayeAccount(s"/paye/$nino", Nino(nino)))), None, None, CreationAndLastModifiedDetail())

  def agentAuthority(id: String, uar: String): Authority =
    Authority(s"/auth/oid/$id", Credentials(), Accounts(agent = Some(AgentAccount(s"/agent/$uar", Uar(uar)))), None, None, CreationAndLastModifiedDetail())

  def payeAndAgentAuthority(id: String, nino: String, uar: String): Authority =
    Authority(s"/auth/oid/$id", Credentials(), Accounts(paye = Some(PayeAccount(s"/paye/$nino", Nino(nino))), agent = Some(AgentAccount(s"/agent/$uar", Uar(uar)))), None, None, CreationAndLastModifiedDetail())

  def saAuthority(id: String, utr: String): Authority =
    Authority(s"/auth/oid/$id", Credentials(), Accounts(sa = Some(SaAccount(s"/sa/individual/$utr", SaUtr(utr)))), None, None, CreationAndLastModifiedDetail())

  def vatAuthority(id: String, vrn: String): Authority =
    Authority(s"/auth/oid/$id", Credentials(), Accounts(vat = Some(VatAccount(s"/vat/$vrn", Vrn(vrn)))), None, None, CreationAndLastModifiedDetail())

  def ctAuthority(id: String, utr: String): Authority =
    Authority(s"/auth/oid/$id", Credentials(), Accounts(ct = Some(CtAccount(s"/ct/$utr", CtUtr(utr)))), None, None, CreationAndLastModifiedDetail())

  def epayeAuthority(id: String, empRef: String): Authority =
    Authority(s"/auth/oid/$id", Credentials(), Accounts(epaye = Some(EpayeAccount(s"/epaye/$empRef", EmpRef(empRef)))), None, None, CreationAndLastModifiedDetail())

  def allBizTaxAuthority(id: String, saUtr: String, ctUtr: String, vrn: String, empRef: String) =
    Authority(s"/auth/oid/$id", Credentials(), Accounts(
      sa = Some(SaAccount(s"/sa/individual/$saUtr", SaUtr(saUtr))),
      vat = Some(VatAccount(s"/vat/$vrn", Vrn(vrn))),
      ct = Some(CtAccount(s"/ct/$ctUtr", CtUtr(ctUtr))),
      epaye = Some(EpayeAccount(s"/epaye/$empRef", EmpRef(empRef)))),
     None, None, CreationAndLastModifiedDetail())

  def emptyAuthority(id: String) = Authority(s"/auth/oid/$id", Credentials(), Accounts(), None, None, CreationAndLastModifiedDetail())
}
