package microservice.auth.domain

case class UserAuthority(id: String, regimes: Map[String, String])
