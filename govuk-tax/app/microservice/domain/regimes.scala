package microservice.domain

import microservice.auth.domain.{ Regimes, UserAuthority }

import microservice.paye.domain.PayeRoot
import microservice.sa.domain.SaRoot
import org.joda.time.DateTime
import play.api.mvc.{ Call, AnyContent, Action, Result }

abstract class TaxRegime {
  def isAuthorised(regimes: Regimes): Boolean

  def unauthorisedLandingPage: Call
}

abstract class RegimeRoot

case class User(user: String, userAuthority: UserAuthority, regimes: RegimeRoots, nameFromGovernmentGateway: Option[String] = None, decryptedToken: Option[String])

case class RegimeRoots(paye: Option[PayeRoot], sa: Option[SaRoot], vat: Option[String]) {
  def hasBusinessTaxRegime: Boolean = sa.isDefined || vat.isDefined
}
