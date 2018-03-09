package controllers


import config.YtaConfig
import play.api.Logger
import play.api.Play.current
import play.api.i18n.Lang
import play.api.mvc.{Action, AnyContent, LegacyI18nSupport}
import uk.gov.hmrc.play.frontend.controller.FrontendController
import play.api.i18n.Messages.Implicits._


trait LanguageController extends FrontendController  with LegacyI18nSupport {

  val english = Lang("en")
  val welsh = Lang("cy")

  def switchToEnglish: Action[AnyContent] = switchToLang(english)
  def switchToWelsh: Action[AnyContent] = switchToLang(welsh)

  private def switchToLang(newLang: Lang) = Action { implicit request =>
    request.headers.get(REFERER) match {
      case Some(referrer) => Redirect(referrer).withLang(newLang)
      case None =>
        Logger.warn(s"Unable to get the referrer, so sending them to ${YtaConfig.fallbackURLForLanguageSwitcher}")
        Redirect(YtaConfig.fallbackURLForLanguageSwitcher).withLang(newLang)
    }
  }

}


object LanguageController extends LanguageController