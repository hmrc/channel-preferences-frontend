package microservice.saml.domain

case class AuthRequestFormData(idaUrl: String, samlRequest: String)

case class AuthResponseValidationData(authResponse: String)

case class AuthResponseValidationResult(valid: Boolean, hashPid: Option[String])
