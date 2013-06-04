package microservice.auth.domain

import java.net.URI

case class PersonalTaxRegimes(paye: Option[URI], sa: Option[URI])

case class UserRegimes(personal: Option[PersonalTaxRegimes])

case class UserAuthority(regimes: UserRegimes)

case class MatsUserAuthority(regimes: Map[String, String])
