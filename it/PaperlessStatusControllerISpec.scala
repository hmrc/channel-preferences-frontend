import uk.gov.hmrc.http.SessionKeys
/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

class PaperlessStatusControllerISpec extends EmailSupport with SessionCookieEncryptionSupport {

  "/paperless/status" should {
    "return 401 when no auth is found" in {
      `/paperless/status`.get().futureValue.status mustBe 401
    }

    "return 200 when a request is authenticated" in {
      val (_, utr): (String, String) = ggAuthHeaderWithUtr
      val response = `/paperless/status`.withSession(
        SessionKeys.authToken -> utr
      ).get().futureValue
      response.status mustBe 200
    }

  }
}
