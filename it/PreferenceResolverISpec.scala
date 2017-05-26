import play.api.test.Helpers._

class PreferenceResolverISpec extends PreferencesFrontEndServer with EmailSupport {

  "a Nino only user logs in with no preference it" should {

    "be redirected to the default optIn page" in new TestCaseWithFrontEndAuthentication {
      val response = `/paperless/activate/:service`("default",utr).put().futureValue
      response.status should be (PRECONDITION_FAILED)
      (response.json \ "redirectUserTo").as[String] should be (s"http://localhost:9024/paperless/choose?returnUrl=$encryptedReturnUrl&returnLinkText=$encryptedReturnText")
      (response.json \ "optedIn").asOpt[Boolean] shouldBe empty
      (response.json \ "verifiedEmail").asOpt[Boolean] shouldBe empty
    }
  }

  "On PTA/BTA " when {
    "a Nino only user logs in with no preference it" should {

      "be redirected to the default optIn page" in new TestCaseWithFrontEndAuthentication {
        val response = `/paperless/activate/:service`("default",utr).put().futureValue
        response.status should be (PRECONDITION_FAILED)
        (response.json \ "redirectUserTo").as[String] should be (s"http://localhost:9024/paperless/choose?returnUrl=$encryptedReturnUrl&returnLinkText=$encryptedReturnText")
        (response.json \ "optedIn").asOpt[Boolean] shouldBe empty
        (response.json \ "verifiedEmail").asOpt[Boolean] shouldBe empty
      }
    }

    "a Nino only user logs with preference for default" should {
      "continue" in {
      }
    }

    "a Nino only user logs with preference for taxCredits" should {
      "be redirected to the default optIn page" in {

      }
    }

    "a SaUtr only user logs in with no preference" should {
      "be redirected to the default optIn page when he logs in on PTA/BTA" in {

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
      "continue" in {
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
