package controllers.common.actions

import controllers.common.service.MicroServices
import controllers.common.{ HeaderNames, CookieEncryption }
import play.api.mvc._
import uk.gov.hmrc.microservice.domain.User
import scala.Some

trait MultiFormWrapper extends MicroServices with CookieEncryption with HeaderNames {
  self: Controller =>

  object MultiFormAction {

    def apply(config: User => MultiFormConfiguration)(action: (User => Request[AnyContent] => Result)): (User => Request[AnyContent] => Result) = {
      implicit user =>
        implicit request =>
          val conf = config(user)
          keyStoreMicroService.getDataKeys(conf.id, conf.source) match {
            case None => Redirect(conf.unauthorisedStep)
            case Some(dataKeys) =>
              if (dataKeys.isEmpty) Redirect(conf.unauthorisedStep)
              else {
                val previousSteps = conf.stepsList.takeWhile(x => x != conf.currentStep).toSet
                if (dataKeys.intersect(previousSteps) == previousSteps) {
                  action(user)(request)
                } else {
                  Redirect(conf.unauthorisedStep)
                }
              }
          }

    }
  }

}

case class MultiFormConfiguration(id: String, source: String, stepsList: List[String], currentStep: String, unauthorisedStep: Call)

