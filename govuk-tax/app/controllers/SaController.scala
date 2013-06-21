package controllers

import microservice.sa.domain.{ SaPerson, SaRoot }

class SaController extends BaseController with ActionWrappers {

  def home = AuthorisedForAction {
    implicit user =>
      implicit request =>

        //        println("user: " + user)
        //        println("user.regimes: " + user.regimes)
        //        println("user.regimes.sa: " + user.regimes.sa)
        val userData: SaRoot = user.regimes.sa.get
        //        println("userData: " + userData)
        val personalDetails = userData.personalDetails.get

        //        println("_________________________personal details: " + personalDetails)
        //        println("_________________________personal details class: " + personalDetails.getClass)
        //        println("_________________________name: " + personalDetails.name)

        Ok(views.html.sa_home(userData.utr, personalDetails))
  }

}
