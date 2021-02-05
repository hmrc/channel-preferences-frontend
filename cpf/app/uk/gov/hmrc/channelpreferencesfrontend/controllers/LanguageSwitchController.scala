/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.channelpreferencesfrontend.controllers

import com.google.inject.Inject
import javax.inject.Singleton
import play.api.i18n.Lang
import play.api.mvc._
import uk.gov.hmrc.channelpreferencesfrontend.config.AppConfig
import uk.gov.hmrc.channelpreferencesfrontend.models.Language
import uk.gov.hmrc.play.language.{ LanguageController, LanguageUtils }

@Singleton
class LanguageSwitchController @Inject() (appConfig: AppConfig, languageUtils: LanguageUtils, cc: ControllerComponents)
    extends LanguageController(languageUtils, cc) {
  import appConfig._

  override def fallbackURL: String =
    "https://www.gov.uk/government/organisations/hm-revenue-customs"

  def selectLanguage(language: Language): Action[AnyContent] = super.switchToLanguage(language.lang.code)

  override protected def languageMap: Map[String, Lang] =
    if (appConfig.languageTranslationEnabled)
      Map(en -> Lang(en), cy -> Lang(cy))
    else
      Map(en -> Lang(en))
}
