/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.channelpreferencesfrontend.models

import cats.implicits.catsSyntaxEitherId
import play.api.i18n.Lang
import play.api.mvc.PathBindable

sealed trait Language {
  val lang: Lang
  val string: String
}

object Language {

  case object Cymraeg extends WithName("cymraeg") with Language {
    override val lang: Lang = Lang("cy")
  }

  case object English extends WithName("english") with Language {
    override val lang: Lang = Lang("en")
  }

  implicit def pathBindable: PathBindable[Language] =
    new PathBindable[Language] {
      override def bind(key: String, value: String): Either[String, Language] =
        value match {
          case Cymraeg.string => Cymraeg.asRight[String]
          case English.string => English.asRight[String]
          case _              => "Invalid language".asLeft[Language]
        }

      override def unbind(key: String, value: Language): String =
        value.string
    }
}
