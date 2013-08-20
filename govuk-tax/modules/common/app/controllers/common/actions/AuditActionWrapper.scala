package controllers.common.actions

import controllers.common.service.MicroServices
import play.api.mvc.{ Result, Action, AnyContent, Controller }

trait AuditActionWrapper extends MicroServices {
  self: Controller =>

  object WithRequestAuditing {

    def apply(action: Action[AnyContent]) = Action {
      request =>
        {
          Ok("")
        }
    }
  }

}
