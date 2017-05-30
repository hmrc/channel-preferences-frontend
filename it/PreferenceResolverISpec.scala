import play.api.http.Status
import play.api.test.Helpers._

class PreferenceResolverISpec extends PreferencesFrontEndServer with EmailSupport {

  val defaultService = "default"
  val taxCreditsService = "taxCredits"

  "On PTA/BTA " when {
    val serviceToLoginOnPtaBta = defaultService

    "a Nino only user logs in with no preference it" should {
      "be redirected to the default optIn page" in new TestCaseWithFrontEndAuthentication {
        val response = `/paperless/:service/activate`(serviceToLoginOnPtaBta)(nino).put().futureValue
        response.status should be(PRECONDITION_FAILED)
        (response.json \ "redirectUserTo").as[String] should be(s"http://localhost:9024/paperless/default/choose?returnUrl=$encryptedReturnUrl&returnLinkText=$encryptedReturnText")
      }
    }

    "a Nino only user logs with preference for default" should {
      "continue by getting the existing preference" in new TestCaseWithFrontEndAuthentication {
        `/preferences/:taxIdName/:taxId/:service`(defaultService, nino)(nino).put().futureValue
        val response = `/paperless/:service/activate`(serviceToLoginOnPtaBta)(nino).put().futureValue
        response.status should be(OK)
      }
    }

    "a Nino only user logs with preference for taxCredits" should {
      "be redirected to the default optIn page" in new TestCaseWithFrontEndAuthentication {
        `/preferences/:taxIdName/:taxId/:service`(taxCreditsService, nino)(nino).put().futureValue
        val response = `/paperless/:service/activate`(serviceToLoginOnPtaBta)(nino).put().futureValue
        response.status should be(PRECONDITION_FAILED)

        // Do we prepopulate the email address?
        (response.json \ "redirectUserTo").as[String] should be(s"http://localhost:9024/paperless/default/choose?returnUrl=$encryptedReturnUrl&returnLinkText=$encryptedReturnText")
      }
    }

    "a SaUtr only user logs in with no preference" should {
      "be redirected to the default optIn page" in new TestCaseWithFrontEndAuthentication {
        val response = `/paperless/:service/activate`(serviceToLoginOnPtaBta)(utr).put().futureValue
        response.status should be(PRECONDITION_FAILED)
        (response.json \ "redirectUserTo").as[String] should be(s"http://localhost:9024/paperless/default/choose?returnUrl=$encryptedReturnUrl&returnLinkText=$encryptedReturnText")
      }
    }

    "a SaUtr only user logs in with default" should {
      "continue by getting the existing preference" in new TestCaseWithFrontEndAuthentication {
        `/preferences/:taxIdName/:taxId/:service`(defaultService, utr)(utr).put().futureValue
        val response = `/paperless/:service/activate`(serviceToLoginOnPtaBta)(utr).put().futureValue
        response.status should be(OK)
      }
    }

    "a SaUtr and Nino user logs in with no preferences for none of them" should {
      "be redirected to the default optIn page when he logs in as SaUtr" in new TestCaseWithFrontEndAuthentication {
        val response = `/paperless/:service/activate`(serviceToLoginOnPtaBta)(nino, utr).put().futureValue
        response.status should be(PRECONDITION_FAILED)
        (response.json \ "redirectUserTo").as[String] should be(s"http://localhost:9024/paperless/default/choose?returnUrl=$encryptedReturnUrl&returnLinkText=$encryptedReturnText")
      }
    }

    "a SaUtr and Nino user logs in with SaUtr and Nino preference for default" should {
      "continue" in new TestCaseWithFrontEndAuthentication {
        `/preferences/:taxIdName/:taxId/:service`(defaultService, nino)(nino, utr).put().futureValue
        `/preferences/:taxIdName/:taxId/:service`(defaultService, utr)(nino, utr).put().futureValue
        val response = `/paperless/:service/activate`(serviceToLoginOnPtaBta)(nino, utr).put().futureValue
        response.status should be(OK)
      }
    }

    "a SaUtr and Nino user logs in with SaUtr preference only" should {
      "be auto enrolled as nino user to default service when he logs in" in new TestCaseWithFrontEndAuthentication {
        `/preferences/:taxIdName/:taxId/:service`(defaultService, utr)(utr).put().futureValue
        `/preferences/:taxIdName/:taxIdValue`(utr)(nino, utr).get().futureValue.status shouldBe Status.OK
        `/preferences/:taxIdName/:taxIdValue`(nino)(nino, utr).get().futureValue.status shouldBe Status.NOT_FOUND

        val response = `/paperless/:service/activate`(serviceToLoginOnPtaBta)(nino, utr).put().futureValue

        response.status should be(OK)
        `/preferences/:taxIdName/:taxIdValue`(nino)(nino, utr).get().futureValue.status shouldBe Status.OK
        `/preferences/:taxIdName/:taxIdValue`(nino)(nino, utr).get(defaultService).futureValue shouldBe true
      }
    }

    "a SaUtr and Nino user logs in with Nino preference only for default" should {
      "be auto enrolled as SaUtr user to default service" in new TestCaseWithFrontEndAuthentication {
        `/preferences/:taxIdName/:taxId/:service`(defaultService, nino)(nino).put().futureValue
        `/preferences/:taxIdName/:taxIdValue`(nino)(nino, utr).get().futureValue.status shouldBe Status.OK
        `/preferences/:taxIdName/:taxIdValue`(utr)(nino, utr).get().futureValue.status shouldBe Status.NOT_FOUND

        val response = `/paperless/:service/activate`(serviceToLoginOnPtaBta)(nino, utr).put().futureValue
        response.status should be(OK)
        `/preferences/:taxIdName/:taxIdValue`(utr)(nino, utr).get().futureValue.status shouldBe Status.OK
        `/preferences/:taxIdName/:taxIdValue`(utr)(nino, utr).get(defaultService).futureValue shouldBe true
      }
    }

    "a SaUtr and Nino user logs in with Nino preference only for taxCredits" should {
      "be redirected to the default optIn page" in new TestCaseWithFrontEndAuthentication {
        `/preferences/:taxIdName/:taxId/:service`(taxCreditsService, nino)(nino, utr).put().futureValue
        val response = `/paperless/:service/activate`(serviceToLoginOnPtaBta)(utr, nino).put().futureValue
        response.status should be(PRECONDITION_FAILED)
        (response.json \ "redirectUserTo").as[String] should be(s"http://localhost:9024/paperless/default/choose?returnUrl=$encryptedReturnUrl&returnLinkText=$encryptedReturnText")
      }
    }
  }


  "On TaxCredits " when {

    val serviceToLoginOnTaxCredits = taxCreditsService

    "a Nino only user logs in with no preference it" should {
      "be redirected to the taxCredits optIn page" in new TestCaseWithFrontEndAuthentication {
        val response = `/paperless/:service/activate`(serviceToLoginOnTaxCredits)(nino).put().futureValue
        response.status should be(PRECONDITION_FAILED)
        (response.json \ "redirectUserTo").as[String] should be(s"http://localhost:9024/paperless/taxCredits/choose?returnUrl=$encryptedReturnUrl&returnLinkText=$encryptedReturnText")
      }
    }

    "a Nino only user logs with preference for default" should {
      "be redirected to the taxCredits optIn page" in new TestCaseWithFrontEndAuthentication{
        `/preferences/:taxIdName/:taxId/:service`(defaultService, nino)(nino).put().futureValue
        val response = `/paperless/:service/activate`(serviceToLoginOnTaxCredits)(nino).put().futureValue

        response.status should be(PRECONDITION_FAILED)

        // Do we prepopulate the email address?
        (response.json \ "redirectUserTo").as[String] should be(s"http://localhost:9024/paperless/taxCredits/choose?returnUrl=$encryptedReturnUrl&returnLinkText=$encryptedReturnText")
      }
    }

    "a Nino only user logs with preference for taxCredits" should {
      "continue" in new TestCaseWithFrontEndAuthentication {
        `/preferences/:taxIdName/:taxId/:service`(taxCreditsService, nino)(nino).put().futureValue
        val response = `/paperless/:service/activate`(taxCreditsService)(nino).put().futureValue
        response.status should be(OK)
      }
    }

    "a SaUtr only user logs in with no preference" should {
      "not be possible" in new TestCaseWithFrontEndAuthentication {

      }
    }

    "a SaUtr only user logs in with default" should {
      "not be possible" in new TestCaseWithFrontEndAuthentication {
      }
    }

    "a SaUtr and Nino user logs in with no preferences for none of them" should {
      "be redirected to the taxCredits optIn page" in new TestCaseWithFrontEndAuthentication {
        val response = `/paperless/:service/activate`(serviceToLoginOnTaxCredits)(nino, utr).put().futureValue
        response.status should be(PRECONDITION_FAILED)
        (response.json \ "redirectUserTo").as[String] should be(s"http://localhost:9024/paperless/taxCredits/choose?returnUrl=$encryptedReturnUrl&returnLinkText=$encryptedReturnText")
      }
    }

    "a SaUtr and Nino user logs in with SaUtr and nino preference for default" should {
      "be redirected to the taxCredits optIn page" in new TestCaseWithFrontEndAuthentication {
        `/preferences/:taxIdName/:taxId/:service`(defaultService, nino)(nino, utr).put().futureValue
        `/preferences/:taxIdName/:taxId/:service`(defaultService, utr)(nino, utr).put().futureValue

        val response = `/paperless/:service/activate`(serviceToLoginOnTaxCredits)(nino, utr).put().futureValue

        response.status should be(PRECONDITION_FAILED)
        (response.json \ "redirectUserTo").as[String] should be(s"http://localhost:9024/paperless/taxCredits/choose?returnUrl=$encryptedReturnUrl&returnLinkText=$encryptedReturnText")
      }
    }

    "a SaUtr and Nino user logs in with SaUtr default preference only" should {
      "be auto enrolled as Nino user to default service" in new TestCaseWithFrontEndAuthentication {
        // ??? not sure
        `/preferences/:taxIdName/:taxId/:service`(defaultService, utr)(utr).put().futureValue
        `/preferences/:taxIdName/:taxIdValue`(utr)(nino, utr).get().futureValue.status shouldBe Status.OK
        `/preferences/:taxIdName/:taxIdValue`(nino)(nino, utr).get().futureValue.status shouldBe Status.NOT_FOUND

        val response = `/paperless/:service/activate`(serviceToLoginOnTaxCredits)(nino, utr).put().futureValue

        response.status should be(OK)
        `/preferences/:taxIdName/:taxIdValue`(nino)(nino, utr).get().futureValue.status shouldBe Status.OK
        `/preferences/:taxIdName/:taxIdValue`(nino)(nino, utr).get(defaultService).futureValue shouldBe true
      }

      "be redirected to the taxCredits optIn page" in new TestCaseWithFrontEndAuthentication {
        `/preferences/:taxIdName/:taxId/:service`(defaultService, utr)(utr).put().futureValue
        `/preferences/:taxIdName/:taxIdValue`(utr)(nino, utr).get().futureValue.status shouldBe Status.OK
        `/preferences/:taxIdName/:taxIdValue`(nino)(nino, utr).get().futureValue.status shouldBe Status.NOT_FOUND

        val response = `/paperless/:service/activate`(serviceToLoginOnTaxCredits)(nino, utr).put().futureValue

        response.status should be(PRECONDITION_FAILED)
        (response.json \ "redirectUserTo").as[String] should be(s"http://localhost:9024/paperless/taxCredits/choose?returnUrl=$encryptedReturnUrl&returnLinkText=$encryptedReturnText")
      }
    }

    "a SaUtr and Nino user logs in with Nino preference only for default" should {
      "be auto enrolled as SaUtr user to default service" in new TestCaseWithFrontEndAuthentication {
        // ??? not sure
        `/preferences/:taxIdName/:taxId/:service`(defaultService, nino)(nino).put().futureValue
        `/preferences/:taxIdName/:taxIdValue`(nino)(nino, utr).get().futureValue.status shouldBe Status.OK
        `/preferences/:taxIdName/:taxIdValue`(utr)(nino, utr).get().futureValue.status shouldBe Status.NOT_FOUND

        val response = `/paperless/:service/activate`(serviceToLoginOnTaxCredits)(nino, utr).put().futureValue

        response.status should be(OK)
        `/preferences/:taxIdName/:taxIdValue`(utr)(nino, utr).get().futureValue.status shouldBe Status.OK
        `/preferences/:taxIdName/:taxIdValue`(utr)(nino, utr).get(defaultService).futureValue shouldBe true

      }

      "be redirected to the taxCredits optIn page" in new TestCaseWithFrontEndAuthentication {
        `/preferences/:taxIdName/:taxId/:service`(defaultService, nino)(nino).put().futureValue
        `/preferences/:taxIdName/:taxIdValue`(nino)(nino, utr).get().futureValue.status shouldBe Status.OK
        `/preferences/:taxIdName/:taxIdValue`(utr)(nino, utr).get().futureValue.status shouldBe Status.NOT_FOUND

        val response = `/paperless/:service/activate`(serviceToLoginOnTaxCredits)(nino, utr).put().futureValue

        response.status should be(PRECONDITION_FAILED)
        (response.json \ "redirectUserTo").as[String] should be(s"http://localhost:9024/paperless/taxCredits/choose?returnUrl=$encryptedReturnUrl&returnLinkText=$encryptedReturnText")
      }
    }

    "a SaUtr and Nino user logs in with Nino preference only for taxCredits" should {
      "continue" in new TestCaseWithFrontEndAuthentication {
      }
    }
  }
}
