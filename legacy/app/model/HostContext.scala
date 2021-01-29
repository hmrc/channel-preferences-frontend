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

package model

import controllers.internal.OptInCohort
import play.api.Logger
import play.api.mvc.QueryStringBindable

case class HostContext(
  returnUrl: String,
  returnLinkText: String,
  termsAndConditions: Option[String] = None,
  email: Option[String] = None,
  alreadyOptedInUrl: Option[String] = None,
  cohort: Option[OptInCohort] = None
) {
  val isTaxCredits = termsAndConditions.fold(false)(_ == "taxCredits")
}

object HostContext {

  implicit def hostContextBinder(
    implicit stringBinder: QueryStringBindable[Encrypted[String]]): QueryStringBindable[HostContext] =
    new QueryStringBindable[HostContext] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, HostContext]] = {
        val returnUrlResult = stringBinder.bind("returnUrl", params)
        val returnLinkTextResult = stringBinder.bind("returnLinkText", params)
        val termsAndConditionsOptionResult = stringBinder.bind("termsAndConditions", params).liftDecryptedOption
        val emailOptionResult = stringBinder.bind("email", params).liftDecryptedOption
        val alreadyOptedInUrl = stringBinder.bind("alreadyOptedInUrl", params).liftDecryptedOption
        val languageResult = stringBinder.bind("language", params).liftDecryptedOption
        val cohortResult =
          stringBinder.bind("cohort", params).liftDecryptedOption.flatMap(x => OptInCohort.fromId(x.toInt))

        (
          returnUrlResult,
          returnLinkTextResult,
          termsAndConditionsOptionResult,
          emailOptionResult,
          languageResult,
          cohortResult) match {
          case (Some(Right(returnUrl)), Some(Right(returnLinkText)), Some("taxCredits"), None, _, _) =>
            Some(Left("TaxCredits must provide email"))
          case (Some(Right(returnUrl)), Some(Right(returnLinkText)), terms, email, lang, pageType) =>
            Some(
              Right(
                HostContext(
                  returnUrl = returnUrl.decryptedValue,
                  returnLinkText = returnLinkText.decryptedValue,
                  termsAndConditions = terms,
                  email = email,
                  alreadyOptedInUrl = alreadyOptedInUrl,
                  pageType
                )))
          case (maybeReturnUrlError, maybeReturnLinkTextError, _, _, _, _) =>
            val errorMessage = Seq(
              extractError(maybeReturnUrlError, Some("No returnUrl query parameter")),
              extractError(maybeReturnLinkTextError, Some("No returnLinkText query parameter"))
            ).flatten.mkString("; ")
            Logger.error(errorMessage)
            Some(Left(errorMessage))
        }
      }

      private def extractError(maybeError: Option[Either[String, _]], defaultMessage: Option[String]) =
        maybeError match {
          case Some(Left(error)) => Some(error)
          case None              => defaultMessage
          case _                 => None
        }

      override def unbind(key: String, value: HostContext): String = {
        val termsAndEmailString: String = {
          value.termsAndConditions.fold("") { tc =>
            "&" + stringBinder.unbind("termsAndConditions", Encrypted(tc))
          } +
            value.email.fold("") { em =>
              "&" + stringBinder.unbind("email", Encrypted(em))
            }
        }

        stringBinder.unbind("returnUrl", Encrypted(value.returnUrl)) + "&" +
          stringBinder.unbind("returnLinkText", Encrypted(value.returnLinkText)) +
          termsAndEmailString +
          value.cohort.fold("") { c =>
            "&" + stringBinder.unbind("cohort", Encrypted(s"${c.id}"))
          }
      }
    }

  implicit class OptionOps(binderResult: Option[Either[String, Encrypted[String]]]) {
    def liftDecryptedOption: Option[String] = binderResult match {
      case Some(Right(encryptedValue)) => Some(encryptedValue.decryptedValue)
      case _                           => None
    }
  }
}
