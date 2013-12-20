package uk.gov.hmrc.microservice

import uk.gov.hmrc.common.BaseSpec

class ConnectionLoggingSpec extends BaseSpec {
  "formatting nanoseconds" should {
    import ConnectionLogging.formatNs

    "show 1 nanosecond as '1ns'" in {
      formatNs(1) shouldBe "1ns"
    }

    "show 1000 nanoseconds as '1us'" in {
      formatNs(1000) shouldBe "1.000us"
    }

    "show 1050 nanoseconds as '1.050us'" in {
      formatNs(1050) shouldBe "1.050us"
    }

    "show 1,000,000 nanoseconds as '1.000ms'" in {
      formatNs(1000000) shouldBe "1.000ms"
    }

    "show 1,050,050 nanoseconds as '1.050ms'" in {
      formatNs(1050050) shouldBe "1.050ms"
    }

    "show 1,000,000,000 nanoseconds as '1.000s" in {
      formatNs(1000000000) shouldBe "1.000s"
    }

    "show 1,050,050,050 nanoseconds as '1.050s" in {
      formatNs(1050050050) shouldBe "1.050s"
    }
  }
}
