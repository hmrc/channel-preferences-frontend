package uk.gov.hmrc.common.microservice.auth.domain

import org.joda.time.DateTime
import java.net.URI
import uk.gov.hmrc.domain.{CtUtr, EmpRef, Vrn, SaUtr}

case class UserAuthority(id: String,
  regimes: Regimes,
  previouslyLoggedInAt: Option[DateTime] = None,
  saUtr: Option[SaUtr] = None,
  vrn: Option[Vrn] = None,
  ctUtr: Option[CtUtr] = None,
  empRef: Option[EmpRef] = None)

case class Regimes(
  paye: Option[URI] = None,
  sa: Option[URI] = None,
  vat: Option[URI] = None,
  ct: Set[URI] = Set())

