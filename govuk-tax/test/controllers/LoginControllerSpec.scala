package controllers

import test.BaseSpec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.mock.MockitoSugar
import microservice.auth.AuthMicroService

class LoginControllerSpec extends BaseSpec with ShouldMatchers with MockitoSugar {

  import play.api.test.Helpers._

  private val mockAuthMicroService = mock[AuthMicroService]
}
