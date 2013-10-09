package controllers.agent.addClient

import uk.gov.hmrc.common.BaseSpec
import org.scalatest.mock.MockitoSugar
import org.joda.time.LocalDate
import models.agent.addClient.ClientSearch

class SearchClientValidationSpec extends BaseSpec with MockitoSugar {

  import SearchClientController.Validation._
  "The validation rules" should {

    "ensure that nino validation" should {
      "pass with valid number without spaces" in { validateNino("AB123456C") should equal (true) }
      "pass with valid number with spaces" in { validateNino("AB 12 34 56 C") should equal (true) }
      "fail with valid number with leading space" in { validateNino(" AB123456C") should equal (false) }
      "fail with valid number with trailing space" in { validateNino("AB123456C ") should equal (false) }
      "fail with empty string" in { validateNino("") should equal (false) }
      "fail with only space" in { validateNino("    ") should equal (false) }
      "fail with total garbage" in {
        validateNino("XXX") should equal (false)
        validateNino("werionownadefwe") should equal (false)
        validateNino("@£%!)(*&^") should equal (false)
        validateNino("123456") should equal (false)
      }
      "fail with only one starting letter" in {
        validateNino("A123456C") should equal (false)
        validateNino("A1234567C") should equal (false)
      }
      "fail with three starting letters" in {
        validateNino("ABC12345C") should equal (false)
        validateNino("ABC123456C") should equal (false)
      }
      "fail with lowercase letters" in {
        validateNino("ab123456c") should equal (false)
      }
      "fail with less than 6 middle digits" in { validateNino("AB12345C") should equal (false) }
      "fail with more than 6 middle digits" in { validateNino("AB1234567C") should equal (false) }

      "fail if we start with invalid characters" in {
        val invalidStartLetterCombinations = List('D', 'F', 'I', 'Q', 'U', 'V').combinations(2).map(_.mkString("")).toList
        val invalidPrefixes = List("BG", "GB", "NK", "KN", "TN", "NT", "ZZ")
        for (v <- invalidStartLetterCombinations ::: invalidPrefixes) {
          validateNino(v + "123456C") should equal (false)
        }
      }

      "fail if the second letter O" in {
        validateNino("AO123456C") should equal (false)
      }
    }

    "ensure that name validation" should {
      "fail with only whitespace" in {
        validateName(Some("     ")) should be (true)
      }
      "fail with invalid characters" in {
        validateName(Some("alert('take over computer');")) should be (false)
      }
      "pass with allowed characters" in {
        validateName(Some("funky '.-")) should be (true)
      }
      "pass with numbers in a name" in {
        validateName(Some("Fredrick Barrins the 3rd")) should be (true)
      }
    }

    "allow the date of birth not to be entered" in { validateDob(None) should be (true) }

    "ensure that the date of birth of the user is no more than 110 years before now" should {
      val hundredAndTenYearsAgo = LocalDate.now.minusYears(110)
      "fail with a dob more than 110 years in age" in { validateDob(Some(hundredAndTenYearsAgo.minusDays(1))) should be (false) }
      "pass with a dob exactly 110 years old" in      { validateDob(Some(hundredAndTenYearsAgo)) should be (true) }
      "pass with a dob < 110 years" in                { validateDob(Some(hundredAndTenYearsAgo.plusDays(1))) should be (true) }
    }

    "ensure that the date of birth of the user is no less than 16 years before now" should {
      val sixteenYearsAgo = LocalDate.now.minusYears(16)
      "fail with a dob less than 16 years in age" in { validateDob(Some(sixteenYearsAgo.plusDays(1))) should be (false) }
      "pass with a dob exactly 16 years old" in      { validateDob(Some(sixteenYearsAgo)) should be (true) }
      "pass with a dob > 16 years" in                { validateDob(Some(sixteenYearsAgo.minusDays(1))) should be (true) }
    }

    "ensure that the nino and at least two other fields have been filled in" should {
      "Pass with nino, first name, last name and dob" in {
        atLeastTwoOptionalAndAllMandatory(ClientSearch("AB123456C", Some("foo"), Some("bar"), Some(LocalDate.now))) should be (true)
      }
      "Pass with nino, first name and last name" in {
        atLeastTwoOptionalAndAllMandatory(ClientSearch("AB123456C", Some("foo"), Some("bar"), None)) should be (true)
      }
      "pass with nino first and dob" in {
        atLeastTwoOptionalAndAllMandatory(ClientSearch("AB123456C", Some("foo"), None, Some(LocalDate.now))) should be (true)
      }
      "pass with nino last and dob" in {
        atLeastTwoOptionalAndAllMandatory(ClientSearch("AB123456C", None, Some("bar"), Some(LocalDate.now))) should be (true)
      }
      "fail with nino only" in {
        atLeastTwoOptionalAndAllMandatory(ClientSearch("AB123456C", None, None, None)) should be (false)
      }
      "fail with nino and first" in {
        atLeastTwoOptionalAndAllMandatory(ClientSearch("AB123456C", Some("foo"), None, None)) should be (false)
      }
      "fail with nino and last" in {
        atLeastTwoOptionalAndAllMandatory(ClientSearch("AB123456C", None, Some("bar"), None)) should be (false)
      }
      "fail with nino and dob" in {
        atLeastTwoOptionalAndAllMandatory(ClientSearch("AB123456C", None, None, Some(LocalDate.now))) should be (false)
      }
      "fail without nino" in {
        atLeastTwoOptionalAndAllMandatory(ClientSearch(null, Some("foo"), Some("bar"), Some(LocalDate.now))) should be (false)
      }
    }
  }
}
