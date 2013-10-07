package controllers.agent.addClient

import uk.gov.hmrc.common.BaseSpec
import org.scalatest.mock.MockitoSugar
import play.api.test.{FakeRequest, WithApplication}
import play.api.test.Helpers._
import play.api.test.FakeApplication
import org.jsoup.Jsoup
import uk.gov.hmrc.common.microservice.domain.{RegimeRoots, User}
import uk.gov.hmrc.common.microservice.paye.domain.PayeRoot
import org.joda.time.LocalDate
import models.agent.addClient.ClientSearch

class SearchClientControllerSpec extends BaseSpec with MockitoSugar {

  private val controller = new SearchClientController()

  val id = "wshakespeare"
  val authority = s"/auth/oid/$id"
  val uri = "/personal/paye/blah"
  val payeRoot = PayeRoot("CE927349E", 1, "Mr", "Will", None, "Shakespeare", "Will Shakespeare", "1983-01-02", Map(), Map(), Map())
  val user = User(id, null, RegimeRoots(Some(payeRoot), None, None, None, None), None, None)

  "Given that Bob is on the search screen the page" should {
    "show errors on the form when we make a submission with no values" in new WithApplication(FakeApplication()) {
      val result = executeSearchActionWith(nino="", firstName="", lastName="", dob=("", "", ""))

      status(result) shouldBe 400

      val doc = Jsoup.parse(contentAsString(result))
      doc.select(".error #nino") should not be 'empty
    }

    "show errors on the form when we make a submission with invalid values" in new WithApplication(FakeApplication()) {
      val result = executeSearchActionWith(nino="XXX", firstName="123", lastName="alert('foo')", dob=("1","1", LocalDate.now().minusYears(111).getYear.toString))

      status(result) shouldBe 400

      val doc = Jsoup.parse(contentAsString(result))
      doc.select(".error #nino") should not be 'empty
      doc.select(".error #firstName") should not be ('empty)
      doc.select(".error #lastName") should not be ('empty)
      doc.select(".error select") should not be 'empty     //TODO: Due to id with . we have this selection instead of #dob.date, not perfect
    }

    "show global error on the form when we fill in nino and only one other field" in new WithApplication(FakeApplication()) {
      val result = executeSearchActionWith(nino="AB123456C", firstName="hasNoValidation", lastName="", dob=("", "", ""))

      status(result) shouldBe 400

      val doc = Jsoup.parse(contentAsString(result))
      doc.select(".error #nino") should be ('empty)
      doc.select(".error #globalErrors") should not be 'empty     //TODO: Due to id with . we have this selection instead of #dob.date, not perfect
    }

    "not show any errors on the form when we make a submission with valid nino, firstName, lastName, dob" in new WithApplication(FakeApplication()) {
      val result = executeSearchActionWith(nino="AB123456C", firstName="firstName", lastName="lastName", dob=("1","1", "1990"))

      status(result) shouldBe 200

      val doc = Jsoup.parse(contentAsString(result))
      doc.select("#clientSearchResults #nino").text should include ("AB123456C")
      doc.select("#clientSearchResults #name").text should include ("firstName lastName")
      doc.select("#clientSearchResults #dob").text should include ("January 1, 1990")
    }

    "not show any errors on the form when we make a submission with valid nino, firstName, lastName" in new WithApplication(FakeApplication()) {
      val result = executeSearchActionWith(nino="AB123456C", firstName="firstName", lastName="lastName", dob=("","", ""))

      status(result) shouldBe 200

      val doc = Jsoup.parse(contentAsString(result))
      doc.select("#clientSearchResults #nino").text should include ("AB123456C")
      doc.select("#clientSearchResults #firstName").text should include ("firstName")
      doc.select("#clientSearchResults #lastName").text should include ("lastName")
      doc.select("#clientSearchResults #dob") should be ('empty)
    }

    "not show any errors on the form when we make a submission with valid nino, firstName, dob" in new WithApplication(FakeApplication()) {
      val result = executeSearchActionWith(nino="AB123456C", firstName="firstName", lastName="", dob=("1","1", "1990"))

      status(result) shouldBe 200

      val doc = Jsoup.parse(contentAsString(result))
      doc.select("#clientSearchResults #nino").text should include ("AB123456C")
      doc.select("#clientSearchResults #firstName").text should include ("firstName")
      doc.select("#clientSearchResults #lastName") should be (empty)
      doc.select("#clientSearchResults #dob").text should include ("January 1, 1990")
    }

    "not show any errors on the form when we make a submission with valid nino, lastName, dob" in new WithApplication(FakeApplication()) {
      val result = executeSearchActionWith(nino="AB123456C", firstName="", lastName="lastName", dob=("1","1", "1990"))

      status(result) shouldBe 200

      val doc = Jsoup.parse(contentAsString(result))
      doc.select("#clientSearchResults #nino").text should include ("AB123456C")
      doc.select("#clientSearchResults #firstName") should be (empty)
      doc.select("#clientSearchResults #lastName").text should include ("lastName")
      doc.select("#clientSearchResults #dob").text should include ("January 1, 1990")
    }

    def executeSearchActionWith(nino: String, firstName: String, lastName: String, dob: (String, String, String)) = {
      controller.searchAction(user, FakeRequest().withFormUrlEncodedBody(("nino", nino),
        ("firstName", firstName),
        ("lastName", lastName),
        ("dob.day", dob._1),
        ("dob.month", dob._2),
        ("dob.year", dob._3)))
    }
  }

  "The validation rules" should {

    "ensure that nino validation" should {
      "pass with valid number without spaces" in { controller.validateNino("AB123456C") should equal (true) }
      "pass with valid number with spaces" in { controller.validateNino("AB 12 34 56 C") should equal (true) }
      "fail with valid number with leading space" in { controller.validateNino(" AB123456C") should equal (false) }
      "fail with valid number with trailing space" in { controller.validateNino("AB123456C ") should equal (false) }
      "fail with empty string" in { controller.validateNino("") should equal (false) }
      "fail with only space" in { controller.validateNino("    ") should equal (false) }
      "fail with total garbage" in {
        controller.validateNino("XXX") should equal (false)
        controller.validateNino("werionownadefwe") should equal (false)
        controller.validateNino("@£%!)(*&^") should equal (false)
        controller.validateNino("123456") should equal (false)
      }
      "fail with only one starting letter" in {
        controller.validateNino("A123456C") should equal (false)
        controller.validateNino("A1234567C") should equal (false)
      }
      "fail with three starting letters" in {
        controller.validateNino("ABC12345C") should equal (false)
        controller.validateNino("ABC123456C") should equal (false)
      }
      "fail with less than 6 middle digits" in { controller.validateNino("AB12345C") should equal (false) }
      "fail with more than 6 middle digits" in { controller.validateNino("AB1234567C") should equal (false) }



      "fail if we start with invalid characters" in {
        val invalidStartLetterCombinations = List('D', 'F', 'I', 'Q', 'U', 'V').combinations(2).map(_.mkString("")).toList
        val invalidPrefixes = List("BG", "GB", "NK", "KN", "TN", "NT", "ZZ")
        for (v <- invalidStartLetterCombinations ::: invalidPrefixes) {
          controller.validateNino(v + "123456C") should equal (false)
        }

      }

      "fail if the second letter O" in {
        controller.validateNino("AO123456C") should equal (false)
      }

    }

    "allow the date of birth not to be entered" in { controller.validateDob(None) should be (true) }

    "ensure that the date of birth of the user is no more than 110 years before now" should {
      val hundredAndTenYearsAgo = LocalDate.now.minusYears(110)
      "fail with a dob more than 110 years in age" in { controller.validateDob(Some(hundredAndTenYearsAgo.minusDays(1))) should be (false) }
      "pass with a dob exactly 110 years old" in      { controller.validateDob(Some(hundredAndTenYearsAgo)) should be (true) }
      "pass with a dob < 110 years" in                { controller.validateDob(Some(hundredAndTenYearsAgo.plusDays(1))) should be (true) }
    }

    "ensure that the date of birth of the user is no less than 16 years before now" should {
      val sixteenYearsAgo = LocalDate.now.minusYears(16)
      "fail with a dob less than 16 years in age" in { controller.validateDob(Some(sixteenYearsAgo.plusDays(1))) should be (false) }
      "pass with a dob exactly 16 years old" in      { controller.validateDob(Some(sixteenYearsAgo)) should be (true) }
      "pass with a dob > 16 years" in                { controller.validateDob(Some(sixteenYearsAgo.minusDays(1))) should be (true) }
    }

    "ensure that the nino and at least two other fields have been filled in" should {
      "Pass with nino, first name, last name and dob" in {
        controller.atLeastTwoOptional(ClientSearch("nino", Some("foo"), Some("bar"), Some(LocalDate.now))) should be (true)
      }
      "Pass with nino, first name and last name" in {
        controller.atLeastTwoOptional(ClientSearch("nino", Some("foo"), Some("bar"), None)) should be (true)
      }
      "pass with nino first and dob" in {
        controller.atLeastTwoOptional(ClientSearch("nino", Some("foo"), None, Some(LocalDate.now))) should be (true)
      }
      "pass with nino last and dob" in {
        controller.atLeastTwoOptional(ClientSearch("nino", None, Some("bar"), Some(LocalDate.now))) should be (true)
      }
      "fail with nino only" in {
        controller.atLeastTwoOptional(ClientSearch("nino", None, None, None)) should be (false)
      }
      "fail with nino and first" in {
        controller.atLeastTwoOptional(ClientSearch("nino", Some("foo"), None, None)) should be (false)
      }
      "fail with nino and last" in {
        controller.atLeastTwoOptional(ClientSearch("nino", None, Some("bar"), None)) should be (false)
      }
      "fail with nino and dob" in {
        controller.atLeastTwoOptional(ClientSearch("nino", None, None, Some(LocalDate.now))) should be (false)
      }
    }

  }

//  Acceptance Criteria (Happy Path)
//
//  Agent Bob searches for a client
//
//  Given that Bob is on the search screen
//    And the values entered into the fields are valid
//  When he executes the search
//  And there is a matching client returned from CID
//  And the same matching client returned from NPS
//    Then he should be taken to the results screen
//
//  Agent Bob's search matches more than one client in CID
//
//  Given that Bob is on the search screen
//    And the values entered into the fields are valid
//  When he executes the search
//  And there is more than 1 matching client returned from CID
//  And the second search in NPS with the first record from the return from CID matches the same client returned from NPS
//  Then he should be taken to the results screen
//
//  Acceptance Criteria (Unhappy path)
//
//  Agent Bob's search matches more than one client in NPS




//  Given that Bob is on the search screen
//    And the values entered into the fields are valid
//  When he executes the search
//  And there is one or more matching clients returned from CID
//  And the second search in NPS with the first record from the return from CID matches more than one client in NPS
//  Then he should *not* be taken to the results screen but should remain on the search screen
//  And he should be prompted that the search returned no matches
//
//  Agent Bob searches for a client and finds a match in CID but not NPS
//
//  Given that Bob is on the search screen
//    And the values entered into the fields are valid
//  When he executes the search
//  And there is a matching client returned from CID
//  And there is no matching client returned from NPS
//  Then he should *not* be taken to the results screen but should remain on the search screen
//  And he should be prompted that the search returned no matches
//
//  Agent Bob searches for a client and finds no match in CID
//
//  Given that Bob is on the search screen
//    And the values entered into the fields are valid
//  When he executes the search
//  And there is no matching client returned from CID
//  Then he should *not* be taken to the results screen but should remain on the search screen
//  And he should be prompted that the search returned no matches
//
//  Agent Bob searches for a client with no NINO or an invalid number of secondary search criteria
//
//  Given that Bob is on the search screen
//    When he executes the search without entering a NINO or with an invalid number of secondary criteria
//    Then he should not be taken to the results screen but prompted for a valid NINO and / or at least two secondary criteria
//
//  Agent Bob searches for a client with a DOB not in the past
//
//  Given that Bob is on the search screen
//    When he executes the search with a DOB that isn't in the past
//    Then he should not be taken to the results screen but prompted for a valid DOB
//
//  Agent Bob searches for a client with a DOB where the client was over 110 years or under 16 years
//
//  Given that Bob is on the search screen
//    When he executes the search with a DOB that puts a client at over 110 years of age, or under 16 years or age on the day of the search
//    Then he should not be taken to the results screen but prompted for a valid DOB
//

}
