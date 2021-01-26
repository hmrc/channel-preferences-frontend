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

import play.api.mvc.PathBindable

sealed trait FormType {
  def value: String
}
case object SaAll extends FormType { val value = "sa-all" }
case object NoticeOfCoding extends FormType { val value = "notice-of-coding" }

object FormType {

  private val allFormTypes = Seq(SaAll, NoticeOfCoding)

  implicit val formTypeBinder = new PathBindable[FormType] {

    override def bind(key: String, value: String): Either[String, FormType] =
      allFormTypes.find(_.value == value) map (Right(_)) getOrElse Left(s"$value is not a valid form-type")

    override def unbind(key: String, formType: FormType): String = formType.value
  }
}
