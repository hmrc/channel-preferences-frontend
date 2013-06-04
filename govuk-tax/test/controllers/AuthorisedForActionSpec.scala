package controllers

import test.BaseSpec
import org.scalatest.mock.MockitoSugar
import play.api.test._
import play.api.test.Helpers._
import org.scalatest.matchers.ShouldMatchers
import controllers.service.AuthorityData
import play.api.mvc.{AnyContent, Controller}
import scala.concurrent.Future

class AuthorisedForActionSpec extends BaseSpec with ShouldMatchers with MockitoSugar {

  class TestController(authorityData: AuthorityData) extends Controller with ActionWrappers {

    def test = AuthorisedForAction(authorityData, handler = {
      WithPersonalData(personalTax = mockPersonalTax, handler = { implicit request: PersonalRequest[AnyContent] =>
        Async {
          Future(Ok(request.paye.get.firstName))
        }
      })
    })
  }


  "basic homepage test" should {
    "contain the user's first name in the response"

  }

}
