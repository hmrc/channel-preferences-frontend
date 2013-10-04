package controllers.agent.addClient

import uk.gov.hmrc.common.BaseSpec
import org.scalatest.mock.MockitoSugar
import play.api.test.{FakeRequest, WithApplication}
import play.api.test.Helpers._
import play.api.test.FakeApplication
import org.jsoup.Jsoup
import uk.gov.hmrc.common.microservice.domain.{RegimeRoots, User}
import uk.gov.hmrc.common.microservice.paye.domain.PayeRoot

class SearchClientControllerSpec extends BaseSpec with MockitoSugar {

  private val controller = new SearchClientController()

  val id = "wshakespeare"
  val authority = s"/auth/oid/$id"
  val uri = "/personal/paye/blah"
  val payeRoot = PayeRoot("CE927349E", 1, "Mr", "Will", None, "Shakespeare", "Will Shakespeare", "1983-01-02", Map(), Map())
  val user = User(id, null, RegimeRoots(Some(payeRoot), None, None, None, None), None, None)

  "Given that Bob is on the search screen the page" should {
    "show errors on the form when we get an invalid submission" in new WithApplication(FakeApplication()) {

      private val request = FakeRequest()
      request.withFormUrlEncodedBody()
      val result = controller.searchAction(user, request)

      status(result) shouldBe 200

      val doc = Jsoup.parse(contentAsString(result))
      doc.select(".error #nino") should not be 'empty
      doc.select(".error #firstName") should not be 'empty
      doc.select(".error #lastName") should not be 'empty
      doc.select(".error select") should not be 'empty     //TODO: Due to id with . we have this selection instead of #dob.date, not perfect


      //When he attempts tyo execute the search where there are format validation failures
      //Then he should not be taken to the results screen but prompted to resolve the format validations

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
        for (v <- List('D', 'F', 'I', 'Q', 'U', 'V').combinations(2)) {
          controller.validateNino(v.mkString("") + "123456C") should equal (false)
        }

      }

      //  Neither of . The second letter also cannot be O. The prefixes BG, GB, NK, KN, TN, NT and ZZ are not allocated
//      "fail if any of the first two letters are D, F, I, Q, U or V" in {
//        controller.validateNino("AD123456C") should equal (true)
//        controller.validateNino("AF123456C") should equal (true)
//        controller.validateNino("AI123456C") should equal (true)
//        controller.validateNino("AQ123456C") should equal (true)
//        controller.validateNino("AU123456C") should equal (true)
//        controller.validateNino("AV123456C") should equal (true)
//      }
    }

//    private def validateNino(s: String) = s.matches("\\.+")
//    private def validateLastName(s: String) = s.matches("\\.+")
//    private def validateFirst(s: String) = s.matches("\\.+")

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




//
//    Only the first letter of the client's first name and the first three letters of the client's last name should be used in the search
//
//  DOB rules - maximum age should be 110 based on todays date, minimum age should be 16



}
