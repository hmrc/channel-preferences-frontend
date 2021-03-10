/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

import play.api.test.Helpers._
import uk.gov.hmrc.http.SessionKeys

class PaperlessStatusControllerISpec extends EmailSupport with SessionCookieEncryptionSupport {

  "/paperless/status" should {
    "return 401 when no auth is found" in {
      `/paperless/status`.get().futureValue.status mustBe UNAUTHORIZED
    }

    "return 200 when a request is authenticated" in {
      val (_, utr): (String, String) = ggAuthHeaderWithUtr
      val response = `/paperless/status`.withSession(
        SessionKeys.authToken -> utr
      )().get().futureValue
      response.status mustBe OK
    }

  }
}
