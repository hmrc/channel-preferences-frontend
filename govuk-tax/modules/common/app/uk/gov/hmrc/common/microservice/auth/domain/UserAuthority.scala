package uk.gov.hmrc.common.microservice.auth.domain

import org.joda.time.DateTime
import java.net.URI
import uk.gov.hmrc.domain._
import uk.gov.hmrc.domain.EmpRef
import uk.gov.hmrc.domain.CtUtr
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.domain.Vrn

case class UserAuthorityA(id: String,
  regimes: Regimes,
  previouslyLoggedInAt: Option[DateTime] = None,
  saUtr: Option[SaUtr] = None,
  vrn: Option[Vrn] = None,
  ctUtr: Option[CtUtr] = None,
  empRef: Option[EmpRef] = None,
  nino: Option[Nino] = None,
  uar: Option[Uar] = None,
  governmentGatewayCredential: Option[GovernmentGatewayCredentialResponse] = None,
  idaCredential: Option[IdaCredentialResponse] = None)

case class Regimes(
  paye: Option[URI] = None,
  agent: Option[URI] = None,
  sa: Option[URI] = None,
  vat: Option[URI] = None,
  ct: Option[URI] = None,
  epaye: Option[URI] = None)

case class GovernmentGatewayCredentialResponse(credentialId: String)

case class IdaCredentialResponse(pids: List[Pid])

case class Pid(pid: String) {
  override lazy val toString = pid
}
