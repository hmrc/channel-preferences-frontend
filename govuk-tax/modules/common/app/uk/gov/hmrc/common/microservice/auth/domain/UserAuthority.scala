package uk.gov.hmrc.common.microservice.auth.domain

import org.joda.time.DateTime
import java.net.URI
import uk.gov.hmrc.domain._
import uk.gov.hmrc.domain.EmpRef
import uk.gov.hmrc.domain.CtUtr
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.domain.Vrn

case class UserAuthority(id: String,
  regimes: Regimes,
  previouslyLoggedInAt: Option[DateTime] = None,
  @deprecated("Use the saUtr field - 'utr' will be removed", "18/9/2013") utr: Option[SaUtr] = None, // TODO [JJS] This needs to be renamed to saUtr (but may require changes to JSON from tax-services)
  vrn: Option[Vrn] = None,
  ctUtr: Option[CtUtr] = None,
  empRef: Option[EmpRef] = None,
  nino: Option[Nino],
  governmentGatewayCredential: Option[GovernmentGatewayCredentialResponse],
  idaCredential: Option[IdaCredentialResponse]) {
  val saUtr = utr
}

case class Regimes(
  paye: Option[URI] = None,
  sa: Option[URI] = None,
  vat: Option[URI] = None,
  ct: Set[URI] = Set())

case class GovernmentGatewayCredentialResponse(credentialId: String)

case class IdaCredentialResponse(pids: List[Pid])

case class Pid(pid: String) {
  override lazy val toString = pid
}
