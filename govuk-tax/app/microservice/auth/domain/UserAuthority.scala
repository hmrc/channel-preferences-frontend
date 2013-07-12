package microservice.auth.domain

import org.joda.time.DateTime

case class UserAuthority(id: String, regimes: Regimes, previouslyLoggedInAt: Option[DateTime])
case class Regimes(paye: Option[String] = None, sa: Option[String] = None, vat: Set[String] = Set(), ct: Set[String] = Set())