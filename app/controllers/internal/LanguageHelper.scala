/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package controllers.internal
import model.Language
import model.Language.English
import play.api.libs.json.{ JsError, JsSuccess, Json }

trait LanguageHelper {

  def languageType(language: String): Language =
    Json.toJson(language).validate[Language] match {
      case JsError(_)          => English
      case JsSuccess(value, _) => value
    }

}
