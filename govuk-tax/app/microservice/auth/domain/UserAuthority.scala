package microservice.auth.domain

import org.joda.time.DateTime
import java.net.URI

case class UserAuthority(id: String, regimes: Regimes, previouslyLoggedInAt: Option[DateTime])
case class Regimes(paye: Option[URI] = None, sa: Option[URI] = None, vat: Set[URI] = Set(), ct: Set[URI] = Set())

