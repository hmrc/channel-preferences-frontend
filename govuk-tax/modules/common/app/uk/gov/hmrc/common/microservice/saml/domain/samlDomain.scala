package uk.gov.hmrc.microservice.saml.domain


case class AuthRequestFormData(idaUrl: String, samlRequest: String)

case class AuthResponseValidationData(authResponse: String)

case object Match extends IdaResponse

case object NoMatch extends IdaResponse

case object Cancel extends IdaResponse

case object Error extends IdaResponse

trait IdaResponse {
  override def toString = this.getClass.getSimpleName.split("\\$").last
}

object IdaResponse {
  private val mappings = Map(
    Match.toString -> Match,
    NoMatch.toString -> NoMatch,
    Cancel.toString -> Cancel,
    Error.toString -> Error
  )

  implicit def toString(idaResponse: IdaResponse): String = idaResponse.toString

  implicit def apply(value: String): IdaResponse = {
    mappings.getOrElse(value, throw new IllegalArgumentException(s"Unrecognized Ida response: '$value'"))
  }
}

case class AuthResponseValidationResult(idaResponse: String, hashPid: Option[String], originalRequestId: Option[String])