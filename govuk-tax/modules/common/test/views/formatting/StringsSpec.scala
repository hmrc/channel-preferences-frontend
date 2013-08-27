package views.formatting

import org.scalatest.WordSpec
import org.scalatest.matchers.ShouldMatchers
import play.api.i18n.Lang

class StringsSpec extends WordSpec with ShouldMatchers {

  "Strings" should {
    "convert mixed case text into sentence case" in {
      Strings.sentence("Fred Flintstone") should be("fred flintstone")
    }

    "convert mixed case text into sentence start case" in {
      Strings.sentenceStart("fred Flintstone") should be("Fred flintstone")
    }

    "convert lower case into capital case" in {
      Strings.capitalise("fred flintstone") should be("Fred Flintstone")
    }

    "convert mixed case text into lower case hyphenated text" in {
      Strings.lowerCaseHyphenated("Fred flintstone") should be("fred-flintstone")
    }
  }
}
