package microservice.domain

import microservice.auth.domain.UserAuthority

import microservice.paye.domain.PayeRoot
import microservice.sa.domain.SaRoot

abstract class TaxRegime

abstract class RegimeRoot

case class User(user: String, userAuthority: UserAuthority, regimes: RegimeRoots)

case class RegimeRoots(paye: Option[PayeRoot], sa: Option[SaRoot])
