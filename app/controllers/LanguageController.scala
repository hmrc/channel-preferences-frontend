/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package controllers

import config.YtaConfig
import javax.inject.Inject
import play.api.i18n.Lang
import play.api.mvc.{ Action, AnyContent, LegacyI18nSupport, MessagesControllerComponents }
import play.api.{ Configuration, Logger }
import uk.gov.hmrc.play.bootstrap.config.RunMode
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.ExecutionContext

class LanguageController @Inject()(
  configuration: Configuration,
  ytaConfig: YtaConfig,
  runMode: RunMode,
  mcc: MessagesControllerComponents)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with LegacyI18nSupport {

  val english = Lang("en")
  val welsh = Lang("cy")

  def switchToEnglish: Action[AnyContent] = switchToLang(english)
  def switchToWelsh: Action[AnyContent] = switchToLang(welsh)

  private def switchToLang(newLang: Lang) = Action { implicit request =>
    request.headers.get(REFERER) match {
      case Some(referrer) => Redirect(referrer).withLang(newLang)
      case None =>
        Logger.warn(s"Unable to get the referrer, so sending them to ${ytaConfig.fallbackURLForLanguageSwitcher}")
        Redirect(ytaConfig.fallbackURLForLanguageSwitcher).withLang(newLang)
    }
  }

}
