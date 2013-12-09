package uk.gov.hmrc.microservice

import uk.gov.hmrc.common.BaseSpec

class ConnectionLoggingSpec extends BaseSpec {
  "formatting nanoseconds" should {
    import ConnectionLogging.formatNs

    "show 1 nanosecond as '1 ns'" in {
      formatNs(1) shouldBe "1 ns"
    }

    "show 1000 nanoseconds as '1 us'" in {
      formatNs(1000) shouldBe "1.000 us"
    }

    "show 1050 nanoseconds as '1.050 us'" in {
      formatNs(1050) shouldBe "1.050 us"
    }

    "show 1,000,000 nanoseconds as '1.000 ms'" in {
      formatNs(1000000) shouldBe "1.000 ms"
    }

    "show 1,050,050 nanoseconds as '1.050 ms'" in {
      formatNs(1050050) shouldBe "1.050 ms"
    }

    "show 1,000,000,000 nanoseconds as '1.000 s" in {
      formatNs(1000000000) shouldBe "1.000 s"
    }

    "show 1,050,050,050 nanoseconds as '1.050 s" in {
      formatNs(1050050050) shouldBe "1.050 s"
    }
  }
}
