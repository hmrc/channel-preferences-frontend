package controllers.service

case class SamlFormData(idaUrl: String, samlRequest: String)

//class SamlForm(saml: Saml = new Saml()) extends ResponseHandler {
//
//  import scala.concurrent.Future
//
//  def get: Future[SamlFormData] = response[SamlFormData](saml.samlFormData)
//}
//
//object SamlForm {
//
//  private val samlForm = new SamlForm()
//
//  def apply() = samlForm
//}
