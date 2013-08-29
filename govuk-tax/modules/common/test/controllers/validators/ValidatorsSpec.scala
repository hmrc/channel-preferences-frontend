package controllers.validators

import controllers.common.validators.characterValidator
import uk.gov.hmrc.common.BaseSpec

class ValidatorsSpec extends BaseSpec {

  //Valid Characters Alphanumeric (A-Z, a-z, 0-9), hyphen( - ), apostrophe ( ' ), comma ( , ), forward slash ( / ) ampersand ( & ) and space
  // (48 to 57 0-9) (65 to 90 A-Z) (97 to 122 a-z) (32 space) (38 ampersand ( & )) (39 apostrophe ( ' )) (44 comma ( , ))   (45 hyphen( - )) (47 forward slash ( / ))

  " Valid character checker " should {
    " return false if an invalid character is present in an input " in {
      var digits = for (i <- 48 to 57) yield i
      var lowerCaseLetters = for (i <- 97 to 122) yield i
      var upperCaseLetters = for (i <- 65 to 90) yield i
      val specialCharacters = List(32, 38, 39, 44, 45, 47)

      val validCharacters = digits ++ lowerCaseLetters ++ upperCaseLetters ++ specialCharacters

      for (chr <- 0 to 1000) {
        val c = chr.toChar
        val str = s"this $chr contains $c"
        characterValidator.containsValidAddressCharacters(str) match {
          case true => validCharacters.contains(chr) must be(true)
          case false => validCharacters.contains(chr) must be(false)
        }
      }
    }
    " return true when None is passed as the value" in {
      characterValidator.containsValidAddressCharacters("") must be(true)
    }
  }
}
