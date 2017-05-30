import play.api.libs.json.Json
import play.api.test.Helpers._

class PreferenceResolverISpec extends PreferencesFrontEndServer with EmailSupport {

  "On PTA/BTA " when {
    "a Nino only user logs in with no preference it" should {

      "be redirected to the default optIn page" in new TestCaseWithFrontEndAuthentication {
        val response = `/paperless/:service/activate`("default",nino).put().futureValue
        response.status should be (PRECONDITION_FAILED)
        (response.json \ "redirectUserTo").as[String] should be (s"http://localhost:9024/paperless/default/choose?returnUrl=$encryptedReturnUrl&returnLinkText=$encryptedReturnText")
        (response.json \ "optedIn").asOpt[Boolean] shouldBe empty
        (response.json \ "verifiedEmail").asOpt[Boolean] shouldBe empty
      }
    }

    "a Nino only user logs with preference for default" should {
      "continue by getting the existing preference" in new TestCaseWithFrontEndAuthentication {
        `/preferences/:taxIdName/:taxId/:service`("default", nino, nino).put().futureValue
        val response = `/paperless/:service/activate`("default", nino).put().futureValue
        response.status should be (OK)
        response.json shouldBe Json.parse(
         """{
            | "nino":{"optedIn":true,"terms":"serviceTerms"}
            }""".stripMargin)
      }
    }

    "a Nino only user logs with preference for taxCredits" should {
      "be redirected to the default optIn page" in new TestCaseWithFrontEndAuthentication {
        `/preferences/:taxIdName/:taxId/:service`("taxCredits", nino, nino).put().futureValue
        val response = `/paperless/:service/activate`("default",nino).put().futureValue
        response.status should be (PRECONDITION_FAILED)

        // Do we prepopulate the email address?
        (response.json \ "redirectUserTo").as[String] should be (s"http://localhost:9024/paperless/default/choose?returnUrl=$encryptedReturnUrl&returnLinkText=$encryptedReturnText")
        (response.json \ "optedIn").asOpt[Boolean] shouldBe empty

        // What do we do?
        (response.json \ "verifiedEmail").asOpt[Boolean] shouldBe empty
      }
    }

    "a SaUtr only user logs in with no preference" should {
      "be redirected to the default optIn page" in new TestCaseWithFrontEndAuthentication {
        val response = `/paperless/:service/activate`("default", utr).put().futureValue
        response.status should be (PRECONDITION_FAILED)
        (response.json \ "redirectUserTo").as[String] should be (s"http://localhost:9024/paperless/default/choose?returnUrl=$encryptedReturnUrl&returnLinkText=$encryptedReturnText")
        (response.json \ "optedIn").asOpt[Boolean] shouldBe empty
        (response.json \ "verifiedEmail").asOpt[Boolean] shouldBe empty
      }
    }

    "a SaUtr only user logs in with default" should {
      "continue" in {
      }
    }

    "a SaUtr and Nino user logs in with no preferences for none of them" should {
      "be redirected to the default optIn page when he logs in as SaUtr" in {

      }
    }
    "a SaUtr and Nino user logs in with SaUtr and Nino preference for default" should {
      "continue" in new TestCaseWithFrontEndAuthentication {
        `/preferences/:taxIdName/:taxId/:service`("default", nino, nino, utr).put().futureValue
        `/preferences/:taxIdName/:taxId/:service`("default", utr, nino, utr).put().futureValue
        val response = `/paperless/:service/activate`("default", nino, utr).put().futureValue
        response.status should be (OK)
        response.json shouldBe Json.parse(

          """{
            |    "nino":{"optedIn":true,"terms":"serviceTerms"},
            |    "sautr":{"optedIn":true,"terms":"serviceTerms"}
          }""".stripMargin)
      }
    }
    "a SaUtr and Nino user logs in with SaUtr preference only" should {
      "be auto enrolled as nino user to default service when he logs in" in {

      }
    }

    "a SaUtr and Nino user logs in with Nino preference only for default" should {
      "be auto enrolled as SaUtr user to default service" in {

      }
    }

    "a SaUtr and Nino user logs in with Nino preference only for taxCredits" should {
      "be redirected to the default optIn page" in {
      }
    }
  }


  "On TaxCredits " when {
    "a Nino only user logs in with no preference it" should {
      "be redirected to the taxCredits optIn page" in {

      }
    }

    "a Nino only user logs with preference for default" should {
      "be redirected to the taxCredits optIn page" in {
      }
    }

    "a Nino only user logs with preference for taxCredits" should {
      "continue" in {
      }
    }

    "a SaUtr only user logs in with no preference" should {
      "not be possible" in {

      }
    }

    "a SaUtr only user logs in with default" should {
      "not be possible" in {
      }
    }

    "a SaUtr and Nino user logs in with no preferences for none of them" should {
      "be redirected to the taxCredits optIn page" in {

      }
    }

    "a SaUtr and Nino user logs in with SaUtr and nino preference for default" should {
      "be redirected to the taxCredits optIn page" in {
      }
    }

    "a SaUtr and Nino user logs in with SaUtr default preference only" should {
      "be auto enrolled as Nino user to default service" in {
        // ??? not sure
      }

      "be redirected to the taxCredits optIn page" in {

      }
    }

    "a SaUtr and Nino user logs in with Nino preference only for default" should {
      "be auto enrolled as SaUtr user to default service" in {
        // ??? not sure
      }

      "be redirected to the taxCredits optIn page" in {

      }
    }

    "a SaUtr and Nino user logs in with Nino preference only for taxCredits" should {
      "continue" in {
      }
    }
  }
}
