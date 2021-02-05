/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.channelpreferencesfrontend.config

import javax.inject.{ Inject, Singleton }
import play.api.i18n.Lang
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

@Singleton
class AppConfig @Inject() (servicesConfig: ServicesConfig) {

  lazy val languageTranslationEnabled: Boolean =
    servicesConfig.getBoolean("features.languageTranslationEnabled")

  val en: String = "en"
  val cy: String = "cy"
  val defaultLanguage: Lang = Lang(en)
}
