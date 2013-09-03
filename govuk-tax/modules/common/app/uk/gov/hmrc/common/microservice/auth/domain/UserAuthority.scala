package uk.gov.hmrc.microservice.auth.domain

import org.joda.time.DateTime
import java.net.URI

case class Utr(utr: String) {
  override lazy val toString = utr
}

case class Vrn(vrn: String) {
  override lazy val toString = vrn
}

case class UserAuthority(id: String,
  regimes: Regimes,
  previouslyLoggedInAt: Option[DateTime] = None,
  utr: Option[Utr] = None,
  vrn: Option[Vrn] = None,
  ctUtr: Option[Utr] = None)

case class Regimes(paye: Option[URI] = None,
  sa: Option[URI] = None,
  vat: Set[URI] = Set(),
  ct: Set[URI] = Set())

