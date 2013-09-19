package uk.gov.hmrc.common.microservice.auth.domain

import org.joda.time.DateTime
import java.net.URI
import uk.gov.hmrc.domain.{CtUtr, EmpRef, Vrn, SaUtr}

case class UserAuthority(id: String,
  regimes: Regimes,
  previouslyLoggedInAt: Option[DateTime] = None,
  @deprecated("Use the saUtr field - 'utr' will be removed", "18/9/2013") utr: Option[SaUtr] = None, // TODO [JJS] This needs to be renamed to saUtr (but may require changes to JSON from tax-services)
  vrn: Option[Vrn] = None,
  ctUtr: Option[CtUtr] = None,
  empRef: Option[EmpRef] = None) {
  val saUtr = utr
}

case class Regimes(
  paye: Option[URI] = None,
  sa: Option[URI] = None,
  vat: Option[URI] = None,
  ct: Set[URI] = Set())

