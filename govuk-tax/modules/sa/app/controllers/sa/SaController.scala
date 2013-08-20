package controllers.sa

import play.api.data._
import play.api.data.Forms._

import uk.gov.hmrc.microservice.sa.domain.{ SaRegime, SaPerson, SaRoot }
import views.html.sa.{ sa_prefs_details, sa_personal_details }
import controllers.common.{ SsoPayloadEncryptor, SessionTimeoutWrapper, ActionWrappers, BaseController }
import play.api.libs.json.Json
import config.DateTimeProvider
import controllers.sa.StaticHTMLBanner._

import org.joda.time.{ LocalDate, DateTime }
import uk.gov.hmrc.common.microservice.auth.domain.{ SaPreferences, Preferences }
import controllers.common.service.FrontEndConfig

case class PrintPrefsForm(suppressPrinting: Boolean, email: Option[String], redirectUrl: String)

class SaController extends BaseController with ActionWrappers with SessionTimeoutWrapper with DateTimeProvider {

  val printPrefsForm = Form[PrintPrefsForm](
    mapping(
      "suppressPrinting" -> checked("error.prefs.printsuppression"),
      "email" -> optional(email),
      "redirectUrl" -> text
    )(PrintPrefsForm.apply)(PrintPrefsForm.unapply)
  )

  def details = WithSessionTimeoutValidation(AuthorisedForGovernmentGatewayAction(Some(SaRegime)) {
    implicit user =>
      implicit request =>

        val userData: SaRoot = user.regimes.sa.get

        userData.personalDetails match {
          case Some(person: SaPerson) => Ok(sa_personal_details(userData.utr, person, user.nameFromGovernmentGateway.getOrElse("")))
          case _ => NotFound //todo this should really be an error page
        }
  })

  def checkPrintPreferences(encryptedJson: String) = UnauthorisedAction {
    implicit request =>
      val decryptedJson = SsoPayloadEncryptor.decrypt(encryptedJson)
      val json = Json.parse(decryptedJson)
      //TODO - this needs to change to use the utr not the cred id
      val credId = (json \ "credId").as[String]
      val time = (json \ "time").as[Long]

      val currentTime: DateTime = now()
      if (currentTime.minusMinutes(5).isAfter(time)) BadRequest
      else {
        val headers = ("Access-Control-Allow-Origin", "*")
        authMicroService.preferences(credId) match {
          case None => NoContent
          case Some(pref) => pref.sa match {
            case Some(sa) if sa.digitalNotifications.isDefined => NoContent.withHeaders(headers)
            case _ => Ok(saPreferences(s"${FrontEndConfig.frontendUrl}/sa/prefs")).withHeaders(headers)
          }
        }
      }

  }

  def prefsForm = WithSessionTimeoutValidation(AuthorisedForGovernmentGatewayAction(Some(SaRegime)) {
    implicit user =>
      implicit request =>

        request.queryString.get("rd") match {
          case Some(rd) => Ok(sa_prefs_details(printPrefsForm.fill(PrintPrefsForm(true, None, rd(0)))))
          case _ => NotFound
        }
  })

  def submitPrefsForm() = WithSessionTimeoutValidation(AuthorisedForGovernmentGatewayAction(Some(SaRegime)) {
    implicit user =>
      implicit request =>

        printPrefsForm.bindFromRequest()(request).fold(
          errors => BadRequest(sa_prefs_details(errors)),
          printPrefsForm => {
            val authResponse = authMicroService.savePreferences(user.user, Preferences(sa = Some(SaPreferences(Some(printPrefsForm.suppressPrinting), printPrefsForm.email))))
            authResponse match {
              case Some(_) => Redirect(printPrefsForm.redirectUrl)
              case _ => NotFound //todo this should really be an error page
            }
          }
        )

  })
}
