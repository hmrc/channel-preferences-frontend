/*
 * Copyright 2021 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers

import config.YtaConfig
import play.api.i18n.Lang
import play.api.mvc.{ Action, AnyContent, LegacyI18nSupport, MessagesControllerComponents }
import play.api.{ Configuration, Environment, Logger }
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class LanguageController @Inject()(
  configuration: Configuration,
  ytaConfig: YtaConfig,
  env: Environment,
  mcc: MessagesControllerComponents
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with LegacyI18nSupport {

  val english = Lang("en")
  val welsh = Lang("cy")

  def switchToEnglish: Action[AnyContent] = switchToLang(english)
  def switchToWelsh: Action[AnyContent] = switchToLang(welsh)

  private def switchToLang(newLang: Lang) =
    Action { implicit request =>
      request.headers.get(REFERER) match {
        case Some(referrer) => Redirect(referrer).withLang(newLang)
        case None =>
          Logger.warn(s"Unable to get the referrer, so sending them to ${ytaConfig.fallbackURLForLanguageSwitcher}")
          Redirect(ytaConfig.fallbackURLForLanguageSwitcher).withLang(newLang)
      }
    }

}
