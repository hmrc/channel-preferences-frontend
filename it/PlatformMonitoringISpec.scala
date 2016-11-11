class PlatformMonitoringISpec extends PreferencesFrontEndServer {

  "gif endpoint" should {

    "return 200 for existing asset" in new TestCase {
      val response = `/sa/print-preferences/assets/`("images/tp.gif").futureValue
      response.status should be(200)
    }

    "return 404 for non existing asset" in new TestCase {
      val response = `/sa/print-preferences/assets/`("non_existing_file.gif").futureValue
      response.status should be(404)
    }
  }
}
