package uk.gov.hmrc.microservice.domain

import uk.gov.hmrc.microservice.auth.domain.{ UserAuthority, Regimes }
import uk.gov.hmrc.microservice.paye.domain.PayeRoot
import uk.gov.hmrc.microservice.sa.domain.SaRoot

abstract class TaxRegime {

  def isAuthorised(regimes: Regimes): Boolean

  def unauthorisedLandingPage: String
}

abstract class RegimeRoot

case class User(user: String,
  userAuthority: UserAuthority,
  regimes: RegimeRoots,
  nameFromGovernmentGateway: Option[String] = None,
  decryptedToken: Option[String])

case class RegimeRoots(paye: Option[PayeRoot],
    sa: Option[SaRoot],
    vat: Option[String]) {

  def hasBusinessTaxRegime: Boolean = sa.isDefined || vat.isDefined
}
