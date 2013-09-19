package views.formatting

import org.apache.commons.lang3.text.WordUtils
import play.api.i18n.Messages

object Strings {

  def sentence(value: String) = value.toLowerCase

  def sentenceStart(value: String) =
    value.substring(0, 1).toUpperCase() + sentence(value).substring(1)

  def capitalise(value: String) = WordUtils.capitalizeFully(value)

  def hyphenate(value: String) = value.split(" ").map(sentence(_)).mkString("-")

  def joinList(values: Seq[String], separator: String) = values.mkString(separator)

  def optionalValue(value: Option[String], defaultMessageKey: String) = {
    value match {
      case Some(v) => v
      case None => Messages(defaultMessageKey)
    }
  }
}
