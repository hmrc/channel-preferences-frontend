package controllers.domain

import microservice.auth.domain.UserAuthority

sealed abstract class TaxRegime

class PayeRegime extends TaxRegime

case class PayeDesignatoryDetails(name: String)

sealed abstract class RegimeRoot

case class PayeRoot(designatoryDetails: PayeDesignatoryDetails,
    links: Map[String, String]) extends RegimeRoot {
}

case class User(userAuthority: UserAuthority,
  regime: RegimeRoots)

case class RegimeRoots(paye: Option[PayeRoot])

