package views.formatting

import org.apache.commons.lang3.text.WordUtils

object Strings {

  def sentence(value: String) = value.toLowerCase

  def sentenceStart(value: String) =
    value.substring(0, 1).toUpperCase() + sentence(value).substring(1)

  def capitalise(value: String) = WordUtils.capitalizeFully(value)

  def lowerCaseHyphenated(value: String) = value.split(" ").map(sentence(_)).mkString("-")
}
