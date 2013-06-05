package microservice.saml.domain

case class AuthRequestFormData(idaUrl: String, samlRequest: String)

case class AuthResponseValidationData(authResponse: String)

// not sure what goes into this class
case class AuthResponseValidationResult(valid: Boolean)
