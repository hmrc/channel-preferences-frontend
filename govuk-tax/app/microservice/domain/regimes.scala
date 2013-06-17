package microservice.domain

import microservice.auth.domain.UserAuthority
import microservice.paye.domain.{ SaRoot, PayeRoot }

abstract class TaxRegime

abstract class RegimeRoot

case class User(user: String, userAuthority: UserAuthority, regimes: RegimeRoots)

case class RegimeRoots(paye: Option[PayeRoot], sa: Option[SaRoot])
